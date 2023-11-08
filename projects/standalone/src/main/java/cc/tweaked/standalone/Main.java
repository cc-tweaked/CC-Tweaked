// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.standalone;


import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.core.util.Colour;
import org.apache.commons.cli.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Checks;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL45C.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * A standalone UI for CC: Tweaked computers.
 * <p>
 * This displays a computer terminal using OpenGL and GLFW, without having to load all of Minecraft.
 * <p>
 * The rendering code largely follows that of monitors: we store the terminal data in a TBO, performing the bulk of the
 * rendering logic within the fragment shader ({@code terminal.fsh}).
 */
public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final boolean DEBUG = Checks.DEBUG;

    private record TermSize(int width, int height) {
        public static final TermSize DEFAULT = new TermSize(51, 19);
        public static final Pattern PATTERN = Pattern.compile("^(\\d+)x(\\d+)$");
    }

    private static <T> T getParsedOptionValue(CommandLine cli, String opt, Class<T> klass) throws ParseException {
        var res = cli.getOptionValue(opt);
        if (klass == Path.class) {
            try {
                return klass.cast(Path.of(res));
            } catch (InvalidPathException e) {
                throw new ParseException("'" + res + "' is not a valid path (" + e.getReason() + ")");
            }
        } else if (klass == TermSize.class) {
            var matcher = TermSize.PATTERN.matcher(res);
            if (!matcher.matches()) throw new ParseException("'" + res + "' is not a valid terminal size.");

            return klass.cast(new TermSize(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
        } else {
            return klass.cast(TypeHandler.createValue(res, klass));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        var options = new Options();
        options.addOption(Option.builder("r").argName("PATH").longOpt("resources").hasArg()
            .desc("The path to the resources directory")
            .build());
        options.addOption(Option.builder("c").argName("PATH").longOpt("computer").hasArg()
            .desc("The root directory of the computer. Defaults to a temporary directory.")
            .build());
        options.addOption(Option.builder("t").argName("WIDTHxHEIGHT").longOpt("term-size").hasArg()
            .desc("The size of the terminal, defaults to 51x19")
            .build());

        options.addOption(new Option("h", "help", false, "Print help message"));

        Path resourcesDirectory;
        Path computerDirectory;
        TermSize termSize;
        try {
            var cli = new DefaultParser().parse(options, args);
            if (!cli.hasOption("r")) throw new ParseException("--resources directory is required");

            resourcesDirectory = getParsedOptionValue(cli, "r", Path.class);
            computerDirectory = cli.hasOption("c") ? getParsedOptionValue(cli, "c", Path.class) : null;
            termSize = cli.hasOption("t") ? getParsedOptionValue(cli, "t", TermSize.class) : TermSize.DEFAULT;
        } catch (ParseException e) {
            System.err.println(e.getLocalizedMessage());

            var writer = new PrintWriter(System.err);
            new HelpFormatter().printUsage(writer, HelpFormatter.DEFAULT_WIDTH, "standalone.jar", options);
            writer.flush();

            System.exit(1);
            return;
        }

        var context = ComputerContext.builder(new StandaloneGlobalEnvironment(resourcesDirectory)).build();
        try (var gl = new GLObjects()) {
            var isDirty = new AtomicBoolean(true);
            var computer = new Computer(
                context,
                new StandaloneComputerEnvironment(computerDirectory),
                new Terminal(termSize.width(), termSize.height(), true, () -> isDirty.set(true)),
                0
            );
            computer.turnOn();

            runAndInit(gl, computer, isDirty);
        } catch (Exception e) {
            LOG.error("A fatal error occurred", e);
            System.exit(1);
        } finally {
            context.ensureClosed(1, TimeUnit.SECONDS);
        }
    }

    private static final int SCALE = 2;
    private static final int MARGIN = 2;
    private static final int PIXEL_WIDTH = 6;
    private static final int PIXEL_HEIGHT = 9;

    // Offsets for our shader attributes - see also terminal.vsh.
    private static final int ATTRIBUTE_POSITION = 0;
    private static final int ATTRIBUTE_UV = 1;

    // Offsets for our shader uniforms - see also terminal.fsh.
    private static final int UNIFORM_FONT = 0;
    private static final int UNIFORM_TERMINAL = 1;
    private static final int UNIFORM_TERMINAL_DATA = 0;
    private static final int UNIFORM_CURSOR_BLINK = 2;

    // Offsets for our textures.
    private static final int TEXTURE_FONT = 0;
    private static final int TEXTURE_TBO = 1;

    /**
     * Size of the terminal UBO.
     *
     * @see #setUniformData(ByteBuffer, Terminal)
     * @see #UNIFORM_TERMINAL_DATA
     */
    private static final int TERMINAL_DATA_SIZE = 4 * 4 * 16 + 4 + 4 + 2 * 4 + 4;

    private static void runAndInit(GLObjects gl, Computer computer, AtomicBoolean isDirty) throws IOException {
        var terminal = computer.getEnvironment().getTerminal();
        var inputState = new InputState(computer);

        // Setup an error callback.
        GLFWErrorCallback.createPrint(System.err).set();
        gl.add(() -> Objects.requireNonNull(glfwSetErrorCallback(null)).free());

        // Initialize GLFW.
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");
        gl.add(GLFW::glfwTerminate);

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // Hide the window - we manually show it later.
        glfwWindowHint(GLFW_RESIZABLE, GL_FALSE);// Force the window to remain the terminal size.

        // Configure OpenGL
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 5);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        if (DEBUG) glfwWindowHint(GLFW_CONTEXT_DEBUG, GLFW_TRUE);

        var window = glfwCreateWindow(
            SCALE * (MARGIN * 2 + PIXEL_WIDTH * terminal.getWidth()),
            SCALE * (MARGIN * 2 + PIXEL_HEIGHT * terminal.getHeight()),
            "CC: Tweaked - Standalone", NULL, NULL
        );
        if (window == NULL) throw new RuntimeException("Failed to create the GLFW window");
        gl.add(() -> {
            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
        });

        // Get the window size so we can centre it.
        try (var stack = MemoryStack.stackPush()) {
            var width = stack.mallocInt(1);
            var height = stack.mallocInt(1);
            glfwGetWindowSize(window, width, height);

            // Get the resolution of the primary monitor
            var mode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            if (mode != null) {
                glfwSetWindowPos(window, (mode.width() - width.get(0)) / 2, (mode.height() - height.get(0)) / 2);
            }
        }

        // Add all our callbacks
        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> inputState.onKeyEvent(w, key, action, mods));
        glfwSetCharModsCallback(window, (w, codepoint, mods) -> inputState.onCharEvent(codepoint));
        glfwSetDropCallback(window, (w, count, files) -> inputState.onFileDrop(count, files));
        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> inputState.onMouseClick(button, action));
        glfwSetCursorPosCallback(window, (w, x, y) -> {
            var charX = (int) (((x / SCALE) - MARGIN) / PIXEL_WIDTH);
            var charY = (int) (((y / SCALE) - MARGIN) / PIXEL_HEIGHT);
            charX = Math.min(Math.max(charX, 0), terminal.getWidth() - 1);
            charY = Math.min(Math.max(charY, 0), terminal.getHeight() - 1);
            inputState.onMouseMove(charX, charY);
        });
        glfwSetScrollCallback(window, (w, xOffset, yOffset) -> inputState.onMouseScroll(yOffset));

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // Enable v-sync
        glfwShowWindow(window);

        // Initialise the OpenGL state
        GL.createCapabilities();
        if (DEBUG) {
            GLUtil.setupDebugMessageCallback();
            glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, (int[]) null, true);
        }

        // Load the font texture and bind it.
        var fontTexture = gl.loadTexture("assets/computercraft/textures/gui/term_font.png");
        glBindTextureUnit(TEXTURE_FONT, fontTexture);

        // Create a texture and backing buffer for our TBO and bind it.
        var termBuffer = gl.createBuffer("Terminal TBO");
        var termTexture = gl.createTexture(GL_TEXTURE_BUFFER, "Terminal TBO");
        glTextureBuffer(termTexture, GL_R8UI, termBuffer);
        glBindTextureUnit(TEXTURE_TBO, termTexture);

        // Load the main terminal shader.
        var termProgram = compileProgram(gl);
        glProgramUniform1i(termProgram, UNIFORM_FONT, TEXTURE_FONT);
        glProgramUniform1i(termProgram, UNIFORM_TERMINAL, TEXTURE_TBO);
        glProgramUniform1i(termProgram, UNIFORM_CURSOR_BLINK, 0);

        // Create a backing buffer for our UBO and bind it.
        var termDataBuffer = gl.createBuffer("Terminal Data");
        glBindBufferBase(GL_UNIFORM_BUFFER, UNIFORM_TERMINAL_DATA, termDataBuffer);

        // Create our vertex buffer object. This is just a simple triangle strip of our four corners.
        var termVertices = gl.createBuffer("Terminal Vertices");
        glNamedBufferData(termVertices, new float[]{
            -1.0f, 1.0f, -MARGIN, -MARGIN,
            -1.0f, -1.0f, -MARGIN, PIXEL_HEIGHT * terminal.getHeight() + MARGIN,
            1.0f, 1.0f, PIXEL_WIDTH * terminal.getWidth() + MARGIN, -MARGIN,
            1.0f, -1.0f, PIXEL_WIDTH * terminal.getWidth() + MARGIN, PIXEL_HEIGHT * terminal.getHeight() + MARGIN,
        }, GL_STATIC_DRAW);

        // And our VBA
        var termVertexArray = gl.createVertexArray("Terminal VAO");
        glEnableVertexArrayAttrib(termVertexArray, 0);
        glVertexArrayAttribFormat(termVertexArray, 0, 2, GL_FLOAT, false, 0); // Position
        glEnableVertexArrayAttrib(termVertexArray, 1);
        glVertexArrayAttribFormat(termVertexArray, 1, 2, GL_FLOAT, false, 8); // UV
        // FIXME: Can we merge this into one call?
        glVertexArrayVertexBuffer(termVertexArray, 0, termVertices, 0, 16);
        glVertexArrayVertexBuffer(termVertexArray, 1, termVertices, 0, 16);

        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // We run a single loop for both rendering and ticking computers. This is dubious for all sorts of reason, but
        // good enough for us.
        var lastTickTime = GLFW.glfwGetTime();
        var lastCursorBlink = false;
        while (!glfwWindowShouldClose(window)) {
            // Tick the computer
            computer.tick();
            inputState.update();

            var needRedraw = false;

            // Update the terminal data if needed.
            if (isDirty.getAndSet(false)) {
                needRedraw = true;

                try (var stack = MemoryStack.stackPush()) {
                    var buffer = stack.malloc(terminal.getWidth() * terminal.getHeight() * 3);
                    writeTerminalContents(buffer, terminal);
                    glNamedBufferData(termBuffer, buffer, GL_STATIC_DRAW);
                }

                try (var stack = MemoryStack.stackPush()) {
                    var buffer = stack.malloc(TERMINAL_DATA_SIZE);
                    setUniformData(buffer, terminal);
                    glNamedBufferData(termDataBuffer, buffer, GL_STATIC_DRAW);
                }
            }

            // Update the cursor blink if needed.
            var cursorBlink = terminal.getCursorBlink() && (int) (lastTickTime * 20 / 8) % 2 == 0;
            if (cursorBlink != lastCursorBlink) {
                needRedraw = true;
                glProgramUniform1i(termProgram, UNIFORM_CURSOR_BLINK, cursorBlink ? 1 : 0);
                lastCursorBlink = cursorBlink;
            }

            // Redraw the terminal if needed.
            if (needRedraw) {
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

                glUseProgram(termProgram);
                glBindVertexArray(termVertexArray);
                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                glfwSwapBuffers(window); // swap the color buffers
            }

            // Then wait for the next frame.
            var deadline = lastTickTime + 0.05;
            lastTickTime = GLFW.glfwGetTime();
            while (lastTickTime < deadline) {
                GLFW.glfwWaitEventsTimeout(deadline - lastTickTime);
                lastTickTime = GLFW.glfwGetTime();
            }
        }
    }

    private static int compileProgram(GLObjects gl) throws IOException {
        try (var shaders = new GLObjects()) {
            var vertexShader = shaders.compileShader(GL_VERTEX_SHADER, "terminal.vsh");
            var fragmentShader = shaders.compileShader(GL_FRAGMENT_SHADER, "terminal.fsh");

            var program = gl.createProgram("Terminal program");
            glAttachShader(program, vertexShader);
            glAttachShader(program, fragmentShader);

            glLinkProgram(program);
            if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
                LOG.warn("Error encountered when linking shader: {}", glGetProgramInfoLog(program, 32768));
            }

            return program;
        }
    }

    /**
     * Write the current contents of the terminal to a buffer, ready to be copied to our TBO.
     * <p>
     * Each cell is stored as three packed bytes - character, foreground, background. This is then bound to the
     * {@code Tbo} uniform within the shader, and read to lookup specific the current pixel.
     *
     * @param buffer   The buffer to write to.
     * @param terminal The current terminal.
     */
    private static void writeTerminalContents(ByteBuffer buffer, Terminal terminal) {
        int width = terminal.getWidth(), height = terminal.getHeight();

        var pos = 0;
        for (var y = 0; y < height; y++) {
            TextBuffer text = terminal.getLine(y), textColour = terminal.getTextColourLine(y), background = terminal.getBackgroundColourLine(y);
            for (var x = 0; x < width; x++) {
                buffer.put(pos, (byte) (text.charAt(x) & 0xFF));
                buffer.put(pos + 1, (byte) (15 - Terminal.getColour(textColour.charAt(x), Colour.WHITE)));
                buffer.put(pos + 2, (byte) (15 - Terminal.getColour(background.charAt(x), Colour.BLACK)));
                pos += 3;
            }
        }

        buffer.limit(pos);
    }

    /**
     * Write the additional terminal properties (palette, size, cursor) to a buffer, ready to be copied to our UBO.
     * <p>
     * This is bound to the {@code TermData} uniform, and read to look up terminal-wide properties.
     *
     * @param buffer   The buffer to write to.
     * @param terminal The current terminal.
     */
    private static void setUniformData(ByteBuffer buffer, Terminal terminal) {
        var pos = 0;
        var palette = terminal.getPalette();
        for (var i = 0; i < 16; i++) {
            var colour = palette.getColour(i);
            buffer.putFloat(pos, (float) colour[0]).putFloat(pos + 4, (float) colour[1]).putFloat(pos + 8, (float) colour[2]);

            pos += 4 * 4; // std140 requires these are 4-wide
        }

        var cursorX = terminal.getCursorX();
        var cursorY = terminal.getCursorY();
        var showCursor = terminal.getCursorBlink() && cursorX >= 0 && cursorX < terminal.getWidth() && cursorY >= 0 && cursorY < terminal.getHeight();

        buffer
            .putInt(pos, terminal.getWidth()).putInt(pos + 4, terminal.getHeight())
            .putInt(pos + 8, showCursor ? cursorX : -2)
            .putInt(pos + 12, showCursor ? cursorY : -2)
            .putInt(pos + 16, 15 - terminal.getTextColour());

        buffer.limit(TERMINAL_DATA_SIZE);
    }
}
