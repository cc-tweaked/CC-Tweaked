import { render, h, Component, Computer } from "copycat/embed";
import type { ComponentChild } from "preact";

import settingsFile from "./mount/.settings";
import startupFile from "./mount/startup.lua";
import exprTemplate from "./mount/expr_template.lua";
import exampleImage from "./mount/example.nfp";

const defaultFiles: { [filename: string]: string } = {
    ".settings": settingsFile,
    "startup.lua": startupFile,

    // TODO: Ideally this'd be in data/image.nfp or something, but copy-cat's
    // dir bootstrapping doesn't cope with that right now.
    "test-image.nfp": exampleImage
};

const clamp = (value: number, min: number, max: number): number => {
    if (value < min) return min;
    if (value > max) return max;
    return value;
}

const Click = (options: { run: () => void }) =>
    <button type="button" class="example-run" onClick={options.run}>Run ᐅ</button>

type WindowProps = {};

type WindowState = {
    visible: boolean,

    example: string,
    exampleIdx: number,
}

type Touch = { clientX: number, clientY: number };

class Window extends Component<WindowProps, WindowState> {
    private positioned: boolean = false;
    private left: number = 0;
    private top: number = 0;
    private dragging?: { downX: number, downY: number, initialX: number, initialY: number };

    constructor(props: WindowProps, context: unknown) {
        super(props, context);

        this.state = {
            visible: false,
            example: "",
            exampleIdx: 0,
        }
    }

    componentDidMount() {
        const elements = document.querySelectorAll("pre[data-lua-kind]");
        for (let i = 0; i < elements.length; i++) {
            const element = elements[i] as HTMLElement;

            let example = element.innerText;
            if (element.getAttribute("data-lua-kind") == "expr") {
                example = exprTemplate.replace("__expr__", example);
            }
            render(<Click run={this.runExample(example)} />, element);
        }
    }

    componentDidUpdate(_: WindowProps, { visible }: WindowState) {
        if (!visible && this.state.visible) this.setPosition(this.left, this.top);
    }

    public render(_: WindowProps, { visible, example, exampleIdx }: WindowState): ComponentChild {
        return visible ? <div class="example-window" style={`transform: translate(${this.left}px, ${this.top}px);`}>
            <div class="titlebar">
                <div class="titlebar-drag" onMouseDown={this.onMouseDown} onTouchStart={this.onTouchDown} />
                <button type="button" class="titlebar-close" onClick={this.close}>{"\u2715"}</button>
            </div>
            <div class="computer-container">
                <Computer key={exampleIdx} files={{
                    "example.lua": example, ...defaultFiles
                }} />
            </div>
        </div> : <div class="example-window example-window-hidden" />;
    }

    private runExample(example: string): () => void {
        return () => {
            if (!this.positioned) {
                this.positioned = true;
                this.left = 20;
                this.top = 20;
            }

            this.setState(({ exampleIdx }: WindowState) => ({
                visible: true,
                example: example,
                exampleIdx: exampleIdx + 1,
            }));
        }
    }

    private readonly close = () => this.setState({ visible: false });

    // All the dragging code is terrible. However, I've had massive performance
    // issues doing it other ways, so this'll have to do.
    private onDown(e: Event, touch: Touch) {
        e.stopPropagation();
        e.preventDefault();

        this.dragging = {
            initialX: this.left, initialY: this.top,
            downX: touch.clientX, downY: touch.clientY
        };

        window.addEventListener("mousemove", this.onMouseDrag, true);
        window.addEventListener("touchmove", this.onTouchDrag, true);
        window.addEventListener("mouseup", this.onUp, true);
        window.addEventListener("touchend", this.onUp, true);
    }
    private readonly onMouseDown = (e: MouseEvent) => this.onDown(e, e);
    private readonly onTouchDown = (e: TouchEvent) => this.onDown(e, e.touches[0]);

    private onDrag(e: Event, touch: Touch) {
        e.stopPropagation();
        e.preventDefault();

        const dragging = this.dragging;
        if (!dragging) return;

        this.setPosition(
            dragging.initialX + (touch.clientX - dragging.downX),
            dragging.initialY + (touch.clientY - dragging.downY),
        );
    };
    private readonly onMouseDrag = (e: MouseEvent) => this.onDrag(e, e);
    private readonly onTouchDrag = (e: TouchEvent) => this.onDrag(e, e.touches[0]);

    private readonly onUp = (e: Event) => {
        e.stopPropagation();

        this.dragging = undefined;

        window.removeEventListener("mousemove", this.onMouseDrag, true);
        window.removeEventListener("touchmove", this.onTouchDrag, true);
        window.removeEventListener("mouseup", this.onUp, true);
        window.removeEventListener("touchend", this.onUp, true);
    }

    private readonly setPosition = (left: number, top: number): void => {
        const root = this.base as HTMLElement;

        left = this.left = clamp(left, 0, window.innerWidth - root.offsetWidth);
        top = this.top = clamp(top, 0, window.innerHeight - root.offsetHeight);
        root.style.transform = `translate(${left}px, ${top}px)`;
    }

}

const root = document.createElement("div");
document.body.appendChild(root);
render(<Window />, document.body, root);
