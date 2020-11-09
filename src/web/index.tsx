import { render, h, Component, Computer } from "copycat/embed";
import type { ComponentChild } from "preact";

const Click = ({ run }: { run: () => void }) => {
  return <button type="button" class="example-run" onClick={run}>Run ᐅ</button>
}

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
    const elements = document.querySelectorAll("pre.highlight-lua");
    for (let i = 0; i < elements.length; i++) {
      const element = elements[i] as HTMLElement;

      const example = element.innerText;
      render(<Click run={this.runExample(example)} />, element);
    }

    // Inject <link rel="stylesheet" href="https://copy-cat.squiddev.cc/main.css" />. We could defer this for sure,
    // but it's better to load it at the right time.
    const style = document.createElement("link");
    style.rel = "stylesheet";
    style.href = "https://copy-cat.squiddev.cc/main.css"
    document.head.appendChild(style);
  }

  public render({ }: WindowProps, { visible, example, exampleIdx }: WindowState): ComponentChild {
    return visible ? <div class="example-window" style={`transform: translate(${this.left}px, ${this.top}px);`}>
      <div class="titlebar">
        <div class="titlebar-drag"
          onMouseDown={this.onDown} onTouchStart={this.onTouchDown}
          onMouseMove={this.onDrag} onTouchMove={this.onTouchDrag}
          onMouseUp={this.onUp} onTouchEnd={this.onUp} // onMouseLeave={this.onUp}
        />
        <button type="button" class="titlebar-close" onClick={this.close}>x</button>
      </div>
      <Computer key={exampleIdx} files={{ "startup.lua": example }} />
    </div> : <div class="example-window example-window-hidden" />;
  }

  private runExample(example: string): () => void {
    return () => {
      if (!this.positioned) {
        this.positioned = true;
        this.left = 0;
        this.top = 0;
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
  private readonly onDown = (e: Touch) => {
    this.dragging = {
      initialX: this.left, initialY: this.top,
      downX: e.clientX, downY: e.clientY
    };
  }
  private readonly onTouchDown = (e: TouchEvent) => this.onDown(e.touches[0]);

  private readonly onDrag = (e: Touch) => {
    const dragging = this.dragging;
    if (!dragging) return;

    const root = this.base as HTMLElement;
    const left = this.left = dragging.initialX + (e.clientX - dragging.downX);
    const top = this.top = dragging.initialY + (e.clientY - dragging.downY);
    root.style.transform = `translate(${left}px, ${top}px)`;
  };

  private readonly onTouchDrag = (e: TouchEvent) => this.onDrag(e.touches[0]);

  private readonly onUp = () => this.dragging = undefined;

}

const root = document.createElement("div");
document.body.appendChild(root);
render(<Window />, document.body, root);
