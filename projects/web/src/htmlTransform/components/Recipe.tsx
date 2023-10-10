// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import { type FunctionComponent, type JSX, h } from "preact";
import useExport from "./WithExport";

const Item: FunctionComponent<{ item: string }> = ({ item }) => {
    const data = useExport();
    const itemName = data.itemNames[item];

    return <img
        src={`/images/items/${item.replace(":", "/")}.png`}
        alt={itemName}
        title={itemName}
        className="recipe-icon"
    />;
};

const EmptyItem: FunctionComponent = () => <span className="recipe-icon " />;

const Arrow: FunctionComponent<JSX.IntrinsicElements["svg"]> = props => <svg version="1.1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 45.513 45.512" {...props}>
    <g>
        <path d="M44.275,19.739L30.211,5.675c-0.909-0.909-2.275-1.18-3.463-0.687c-1.188,0.493-1.959,1.654-1.956,2.938l0.015,5.903
   l-21.64,0.054C1.414,13.887-0.004,15.312,0,17.065l0.028,11.522c0.002,0.842,0.338,1.648,0.935,2.242s1.405,0.927,2.247,0.925
   l21.64-0.054l0.014,5.899c0.004,1.286,0.781,2.442,1.971,2.931c1.189,0.487,2.557,0.21,3.46-0.703L44.29,25.694
   C45.926,24.043,45.92,21.381,44.275,19.739z" fill="var(--recipe-hover)" />
    </g>
</svg>;

const Recipe: FunctionComponent<{ recipe: string }> = ({ recipe }) => {
    const data = useExport();
    const recipeInfo = data.recipes[recipe];
    if (!recipeInfo) throw Error("Cannot find recipe for " + recipe);

    return <div className="recipe">
        <strong className="recipe-title">{data.itemNames[recipeInfo.output]}</strong>
        <div className="recipe-inputs">
            {recipeInfo.inputs.map((items, i) => <div className="recipe-item recipe-input" key={i}>{items ? <Item item={items[0]} /> : <EmptyItem />}</div>)}
        </div>
        <Arrow className="recipe-arrow" />
        <div className="recipe-item recipe-output">
            <Item item={recipeInfo.output} />
            {recipeInfo.count > 1 && <span className="recipe-count">{recipeInfo.count}</span>}
        </div>
    </div>;
};

export default Recipe;
