package github.kasuminova.stellarcore.client.gui.font;

import com.github.bsideup.jabel.Desugar;

import java.util.Objects;

@Desugar
public record TextWrapInfo(String info, int wrapWidth) {

    @Override
    public boolean equals(final Object o) {
        if (o instanceof TextWrapInfo renderInfo) {
            return renderInfo.hashCode() == hashCode();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(info, wrapWidth);
    }
}
