package io.proximax.app.fx.control.text;

import org.fxmisc.richtext.model.NodeSegmentOpsBase;

/**
 *
 * @author thcao
 */
public class LinkedImageOps<S> extends NodeSegmentOpsBase<LinkedImage, S> {

    public LinkedImageOps() {
        super(new EmptyLinkedImage());
    }

    @Override
    public int length(LinkedImage linkedImage) {
        return linkedImage.isReal() ? 1 : 0;
    }

}