// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 IViewRotMat;
uniform int FogShape;

out float vertexDistance;
out vec2 fontPos;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1);

    vertexDistance = fog_distance(ModelViewMat, IViewRotMat * Position, FogShape);
    fontPos = UV0;
}
