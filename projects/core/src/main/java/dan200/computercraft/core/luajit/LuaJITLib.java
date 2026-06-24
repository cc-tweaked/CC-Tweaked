// Copyright (c) 2025
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.luajit;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public interface LuaJITLib extends Library {
    LuaJITLib INSTANCE = NativeLuajitLoader.load();

    // State lifecycle
    Pointer luaL_newstate();
    void lua_close(Pointer L);
    void luaL_openlibs(Pointer L);

    // Stack
    int lua_gettop(Pointer L);
    void lua_settop(Pointer L, int idx);
    void lua_pushvalue(Pointer L, int idx);
    void lua_remove(Pointer L, int idx);
    void lua_insert(Pointer L, int idx);

    // Push
    void lua_pushnil(Pointer L);
    void lua_pushnumber(Pointer L, double n);
    void lua_pushinteger(Pointer L, long n);
    void lua_pushstring(Pointer L, String s);
    void lua_pushboolean(Pointer L, int b);

    // Query
    int lua_type(Pointer L, int idx);
    int lua_isnil(Pointer L, int idx);
    int lua_isnumber(Pointer L, int idx);
    int lua_isstring(Pointer L, int idx);
    double lua_tonumberx(Pointer L, int idx, Pointer isnum);
    long lua_tointegerx(Pointer L, int idx, Pointer isnum);
    Pointer lua_tolstring(Pointer L, int idx, Pointer len);
    int lua_toboolean(Pointer L, int idx);

    // Table
    void lua_getglobal(Pointer L, String name);
    void lua_setglobal(Pointer L, String name);
    void lua_getfield(Pointer L, int idx, String k);
    void lua_setfield(Pointer L, int idx, String k);
    void lua_rawgeti(Pointer L, int idx, long n);
    void lua_rawseti(Pointer L, int idx, long n);
    void lua_rawget(Pointer L, int idx);
    void lua_rawset(Pointer L, int idx);
    void lua_newtable(Pointer L);
    int lua_next(Pointer L, int idx);

    // Load & call
    int luaL_loadbufferx(Pointer L, byte[] buff, long sz, String name, String mode);
    int lua_pcall(Pointer L, int nargs, int nresults, int msgh);
    void lua_call(Pointer L, int nargs, int nresults);

    // Coroutine
    Pointer lua_newthread(Pointer L);
    int lua_resume(Pointer L, Pointer from, int nargs);
    int lua_yield(Pointer L, int nresults);
    int lua_status(Pointer L);

    // Hooks
    int lua_sethook(Pointer L, LuaHook hook, int mask, int count);

    // C function registration
    void lua_pushcclosure(Pointer L, LuaCFunction fn, int n);
    void lua_pushcfunction(Pointer L, LuaCFunction fn);

    // Constants
    int LUA_OK = 0;
    int LUA_YIELD = 1;
    int LUA_ERRRUN = 2;
    int LUA_ERRSYNTAX = 3;
    int LUA_ERRMEM = 4;
    int LUA_ERRERR = 5;

    int LUA_TNIL = 0;
    int LUA_TBOOLEAN = 1;
    int LUA_TNUMBER = 3;
    int LUA_TSTRING = 4;
    int LUA_TTABLE = 5;
    int LUA_TFUNCTION = 6;
    int LUA_TTHREAD = 8;

    int LUA_MASKCOUNT = 8;

    @FunctionalInterface
    interface LuaCFunction extends Callback {
        int invoke(Pointer L);
    }

    @FunctionalInterface
    interface LuaHook extends Callback {
        void invoke(Pointer L, Pointer ar);
    }

    // ── Embedded Native Loader ──────────────────────────────────────────

    final class NativeLuajitLoader {
        private static final Logger LOG = LoggerFactory.getLogger(NativeLuajitLoader.class);

        private NativeLuajitLoader() {}

        static LuaJITLib load() {
            var extracted = extractNativeLib();
            if (extracted != null) {
                try {
                    return Native.load(extracted.toAbsolutePath().toString(), LuaJITLib.class);
                } catch (UnsatisfiedLinkError e) {
                    LOG.warn("Embedded lib failed, trying system", e);
                }
            }
            return Native.load("luajit", LuaJITLib.class);
        }

        private static @org.jetbrains.annotations.Nullable Path extractNativeLib() {
            var archDir = archDir();
            var libName = System.mapLibraryName("luajit");
            var resource = "/native/" + archDir + "/" + libName;

            try (InputStream in = LuaJITLib.class.getResourceAsStream(resource)) {
                if (in == null) {
                    LOG.warn("Embedded LuaJIT not found at {}; falling back to system lib", resource);
                    return null;
                }
                var tmpDir = Files.createTempDirectory("cc-luajit-");
                tmpDir.toFile().deleteOnExit();
                var target = tmpDir.resolve(libName);
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                target.toFile().deleteOnExit();
                return target;
            } catch (IOException e) {
                LOG.warn("Failed to extract embedded LuaJIT", e);
                return null;
            }
        }

        private static String archDir() {
            var os = System.getProperty("os.name").toLowerCase();
            var arch = System.getProperty("os.arch").toLowerCase();
            if (os.contains("win")) return "windows-x86-64";
            if (os.contains("mac") || os.contains("darwin"))
                return arch.contains("aarch64") || arch.contains("arm64")
                    ? "darwin-aarch64" : "darwin-x86-64";
            // Linux
            return arch.contains("aarch64") || arch.contains("arm64")
                ? "linux-aarch64" : "linux-x86-64";
        }
    }
}
