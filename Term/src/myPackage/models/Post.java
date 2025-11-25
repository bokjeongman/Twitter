package myPackage.models;

import java.sql.Timestamp;

public class Post {
    private String postId;
    private String writerId;
    private String content;
    private int likes;
    private int commentCount; 
    private int repostCount; 
    private Timestamp createdAt;

    private String originalPostId;
    private String originalWriterId;
    private String originalContent;

    public Post(String postId, String writerId, String content, int likes, int commentCount, int repostCount, Timestamp createdAt,
                String originalPostId, String originalWriterId, String originalContent) {
        this.postId = postId;
        this.writerId = writerId;
        this.content = content;
        this.likes = likes;
        this.commentCount = commentCount; 
        this.repostCount = repostCount;  
        this.createdAt = createdAt;
        this.originalPostId = originalPostId;
        this.originalWriterId = originalWriterId;
        this.originalContent = originalContent;
    }

    // Getters
    public String getPostId() { return postId; }
    public String getWriterId() { return writerId; }
    public int getLikeCount() { return likes; }
    public int getCommentCount() { return commentCount; } 
    
    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }
    public int getRepostCount() { return repostCount; }   
    
    public void setRepostCount(int repostCount) {
        this.repostCount = repostCount;
    }
    public Timestamp getCreatedAt() { return createdAt; }
    
    public String getContent() {
        if (originalPostId != null) {
            if (content != null && !content.trim().isEmpty()) {
                 return content + "\n\n" + 
                        "───────────────\n" +
                        "Quoting @" + originalWriterId + ":\n" + 
                         (originalContent != null ? originalContent : "...");
            } else {
                return "🔁 Reposted @" + originalWriterId + "'s post\n" +
                       "───────────────\n" +
                       (originalContent != null ? originalContent : "...");
            }
        }
        return content;
    }

    public String getOriginalPostId() { return originalPostId; }
    public String getOriginalWriterId() { return originalWriterId; }
    public String getOriginalContent() { return originalContent; }
    
    

    public String toString() {
        return "Post by " + writerId;
    }
}
