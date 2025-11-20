package myPackage.models;

import java.sql.Timestamp;

public class Comment {
    private String commentId;
    private String parentCommentId; // ID of the comment this is replying to
    private String writerId;
    private String content;
    private int likes;
    private Timestamp createdAt;

    public Comment(String commentId, String parentCommentId, String writerId, String content, int likes, Timestamp createdAt) {
        this.commentId = commentId;
        this.parentCommentId = parentCommentId;
        this.writerId = writerId;
        this.content = content;
        this.likes = likes;
        this.createdAt = createdAt;
    }

    // Getters
    public String getCommentId() { return commentId; }
    public String getParentCommentId() { return parentCommentId; }
    public String getWriterId() { return writerId; }
    public String getContent() { return content; }
    public int getLikes() { return likes; }

    @Override
    public String toString() {
        String baseString = "[Comment] " + writerId + ": " + content + " (Likes: " + likes + ")";
        
        if (this.parentCommentId != null) {
            baseString = "    └ [Reply] " + writerId + ": " + content + " (Likes: " + likes + ")";
        }
        return baseString;
    }
}
