package io.proximax.app.fx.control.text;

import javafx.scene.Node;

/**
 *
 * @author thcao
 */
public class EmptyLinkedImage implements LinkedImage {

    @Override
    public boolean isReal() {
        return false;
    }

    @Override
    public String getImagePath() {
        return "";
    }

    @Override
    public Node createNode() {
        throw new AssertionError("Unreachable code");
    }
}