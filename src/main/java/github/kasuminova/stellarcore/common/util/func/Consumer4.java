package github.kasuminova.stellarcore.common.util.func;

@FunctionalInterface
public interface Consumer4<P1, P2, P3, P4> {

    void accept(P1 p1, P2 p2, P3 p3, P4 p4);

    default Consumer4<P1, P2, P3, P4> andThen(Consumer4<? super P1, ? super P2, ? super P3, ? super P4> after) {
        return (p1, p2, p3, p4) -> {
            accept(p1, p2, p3, p4);
            after.accept(p1, p2, p3, p4);
        };
    }

}
