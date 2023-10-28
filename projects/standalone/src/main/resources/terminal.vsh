// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

#version 330 core

layout (location = 0) in vec2 Position;
layout (location = 1) in vec2 UV0;

out vec2 fontPos;

void main() {
    gl_Position = vec4(Position, 0.0, 1.0);
    fontPos = UV0;
}
