package github.kasuminova.stellarcore.client.hitokoto;

@SuppressWarnings("unused")
public class HitokotoResult {
    private int id;
    private String uuid;
    private String hitokoto;
    private String type;
    private String from;
    private String fromWho;
    private String creator;
    private int creatorUid;
    private int reviewer;
    private String commitFrom;
    private String createdAt;
    private int length;

    public int getId() {
        return id;
    }

    public HitokotoResult setId(final int id) {
        this.id = id;
        return this;
    }

    public String getUuid() {
        return uuid;
    }

    public HitokotoResult setUUID(final String uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getHitokoto() {
        return hitokoto;
    }

    @SuppressWarnings("UnusedReturnValue")
    public HitokotoResult setHitokoto(final String hitokoto) {
        this.hitokoto = hitokoto;
        return this;
    }

    public String getType() {
        return type;
    }

    public HitokotoResult setType(final String type) {
        this.type = type;
        return this;
    }

    public String getFrom() {
        return from;
    }

    public HitokotoResult setFrom(final String from) {
        this.from = from;
        return this;
    }

    public String getFromWho() {
        return fromWho;
    }

    public HitokotoResult setFromWho(final String fromWho) {
        this.fromWho = fromWho;
        return this;
    }

    public String getCreator() {
        return creator;
    }

    public HitokotoResult setCreator(final String creator) {
        this.creator = creator;
        return this;
    }

    public int getCreatorUid() {
        return creatorUid;
    }

    public HitokotoResult setCreatorUid(final int creatorUid) {
        this.creatorUid = creatorUid;
        return this;
    }

    public int getReviewer() {
        return reviewer;
    }

    public HitokotoResult setReviewer(final int reviewer) {
        this.reviewer = reviewer;
        return this;
    }

    public String getCommitFrom() {
        return commitFrom;
    }

    public HitokotoResult setCommitFrom(final String commitFrom) {
        this.commitFrom = commitFrom;
        return this;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public HitokotoResult setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public int getLength() {
        return length;
    }

    public HitokotoResult setLength(final int length) {
        this.length = length;
        return this;
    }
}