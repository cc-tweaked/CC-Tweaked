declare module "*.lua" {
    const contents: string;
    export default contents;
}

declare module "*.nfp" {
    const contents: string;
    export default contents;
}

declare module "*.nft" {
    const contents: string;
    export default contents;
}


declare module "*.settings" {
    const contents: string;
    export default contents;
}


declare module "copycat/embed" {
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

    class Computer extends Component<MainProps, unknown> {
        public render(props: MainProps, state: unknown): ComponentChild;
    }

    export { Computer };
}
