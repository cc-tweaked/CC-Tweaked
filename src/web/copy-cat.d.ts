import { h, Component, render, ComponentChild } from "preact";

export { h, Component, render };

export type ComputerAccess = unknown;

export type MainProps = {
  hdFont?: boolean | string,
  persistId?: number,
  files?: { [filename: string]: string | ArrayBuffer },
  label?: string,
  width?: number,
  height?: number,
  resolve?: (computer: ComputerAccess) => void,
}

declare class Computer extends Component<MainProps, unknown> {
  public render(props: MainProps, state: unknown): ComponentChild;
}

export { Computer };
