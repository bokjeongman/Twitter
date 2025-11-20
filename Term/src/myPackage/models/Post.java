package myPackage.models;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

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

    public String getPostId() { return postId; }
    public String getWriterId() { return writerId; }
    public String getContent() { return content; }
    public int getLikes() { return likes; }
    public Timestamp getCreatedAt() { return createdAt; }
    public String getOriginalPostId() { return originalPostId; }
    public String getOriginalWriterId() { return originalWriterId; }
    public String getOriginalContent() { return originalContent; }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String dateStr = sdf.format(createdAt);

        // 1. writer + num_of_likes + timeStamp
        String header = "Writer: " + writerId + " (Likes: " + likes + ")  [" + dateStr + "]";
        
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n");

        // 2. content
        if (originalPostId != null) {
            // repost or quote
            if (content == null || content.isEmpty()) {
                // repost
                sb.append(" [ " + writerId + " Reposted ]");
            } else {
                // quote
                sb.append(content);
            }
            
            // show original content
            sb.append("\n\n")
              .append("    --------------------------------\n")
              .append("    | Original Post by @" + originalWriterId + " |\n")
              .append("    | " + (originalContent != null ? originalContent : "(Content unavailable)") + " |\n")
              .append("    --------------------------------");
        } else {
            sb.append(content != null ? content : "");
        }
        
        return sb.toString();
    }
}
