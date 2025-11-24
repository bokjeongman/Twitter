package myPackage.models;

import java.sql.Timestamp;

public class Post {
    private String postId;
    private String writerId;
    private String content;
    private int likes;
    private Timestamp createdAt;
    private String originalPostId;
    private String originalWriterId;
    private String originalContent;

    public Post(String postId, String writerId, String content, int likes, Timestamp createdAt,
                String originalPostId, String originalWriterId, String originalContent) {
        this.postId = postId;
        this.writerId = writerId;
        this.content = content;
        this.likes = likes;
        this.createdAt = createdAt;
        this.originalPostId = originalPostId;
        this.originalWriterId = originalWriterId;
        this.originalContent = originalContent;
    }

    public String getPostId() { return postId; }
    public String getWriterId() { return writerId; }
    public int getLikeCount() { return likes; }
    public Timestamp getCreatedAt() { return createdAt; }
    
    public String getContent() {
        if (originalPostId != null) {
            // Quote
            if (content != null && !content.trim().isEmpty()) {
                 return content + "\n\n" + 
                        "───────────────\n" +
                        "Quoting @" + originalWriterId + ":\n" + 
                         (originalContent != null ? originalContent : "...");
            } 
            // Repost
            else {
                return "🔁 Reposted @" + originalWriterId + "'s post\n" +
                       "───────────────\n" +
                       (originalContent != null ? originalContent : "...");
            }
        }
        // normal post
        return content;
    }

    public String getOriginalPostId() { return originalPostId; }
    public String getOriginalWriterId() { return originalWriterId; }
    public String getOriginalContent() { return originalContent; }

    @Override
    public String toString() {
        return "Post by " + writerId + ": " + content;
    }
}
