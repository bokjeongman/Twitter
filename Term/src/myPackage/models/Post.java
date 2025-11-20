package myPackage.models;

import java.sql.Timestamp;

public class Post {
    private String postId;
    private String writerId;
    private String content;
    private int likes;
    private Timestamp createdAt;

    // Repost/Quote info
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

    // Getters
    public String getPostId() { return postId; }
    public String getWriterId() { return writerId; }
    public String getContent() { return content; }
    public int getLikes() { return likes; }
    public Timestamp getCreatedAt() { return createdAt; }
    public String getOriginalPostId() { return originalPostId; }
    public String getOriginalWriterId() { return originalWriterId; }
    public String getOriginalContent() { return originalContent; }

    // Format for ListView
    @Override
    public String toString() {
        String postString = "Writer: " + writerId + " (Likes: " + likes + ")\n" +
                            (content != null ? content : "") + "\n" + // Handle null content
                            "(" + createdAt.toString() + ")";
        
        // If this is a Repost or Quote, show original post info
        if (originalPostId != null) {
            if (content == null || content.isEmpty()) { // Repost
                postString = " [ " + writerId + " Reposted ]";
            } else { // Quote
                 postString = " [ " + writerId + " Quoted ]\n" +
                             content + "\n" +
                             "(" + createdAt.toString() + ")";
            }
            
            // Add original content
            postString += "\n\n" +
                          "    [Original Post by @" + originalWriterId + "]\n" +
                          "    " + (originalContent != null ? originalContent : "(Content unavailable)");
        }
        
        return postString;
    }
}