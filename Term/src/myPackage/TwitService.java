package myPackage;

import myPackage.models.Comment;
import myPackage.models.Post;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitService {

    // --- 1. Account ---
    public String login(String id, String pwd) {
        String sql = "select user_id from user where user_id = ? and pwd = ?";
        try (Connection con = DBConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, pwd);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; 
    }

    public boolean signUp(String id, String pwd) {
        String checkSql = "select user_id from user where user_id = ?";
        String insertSql = "insert into user (user_id, pwd) values (?, ?)";
        try (Connection con = DBConnector.getConnection();
             PreparedStatement checkPstmt = con.prepareStatement(checkSql)) {
            checkPstmt.setString(1, id);
            try (ResultSet rs = checkPstmt.executeQuery()) {
                if (rs.next()) return false; 
            }
            try (PreparedStatement insertPstmt = con.prepareStatement(insertSql)) {
                insertPstmt.setString(1, id);
                insertPstmt.setString(2, pwd);
                insertPstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean changePassword(String loggedInUserId, String newPwd) throws SQLException {
        if (newPwd.isEmpty()) return false;
        String sql = "update user set pwd = ? where user_id = ?";
        try (Connection con = DBConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, newPwd);
            pstmt.setString(2, loggedInUserId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteAccount(String userId, String password) throws SQLException {
        String sql = "DELETE FROM user WHERE user_id = ? AND pwd = ?";
        try (Connection con = DBConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, password);
            return pstmt.executeUpdate() > 0;
        }
    }

    // --- 2. Post ---
    
    public List<Post> getTimeline(String loggedInUserId) throws SQLException {
        List<Post> timelinePosts = new ArrayList<>();
        String sql = "SELECT p.post_id, p.writer_id, p.content, p.num_of_likes, p.created_at, " +
                     "       p.original_post_id, op.writer_id AS original_writer_id, op.content AS original_content, " +
                     "       (SELECT COUNT(*) FROM comment c WHERE c.post_id = p.post_id) AS cmt_count, " +
                     "       (SELECT COUNT(*) FROM posts r WHERE r.original_post_id = p.post_id) AS rep_count " +
                     "FROM posts AS p " +
                     "LEFT JOIN posts AS op ON p.original_post_id = op.post_id " + 
                     "WHERE p.writer_id = ? " +  
                     "   OR p.content LIKE ? " + 
                     "   OR p.writer_id IN (SELECT f.following_id FROM follow_relationship AS f WHERE f.user_id = ?) " + 
                     "ORDER BY p.created_at DESC LIMIT 20";
        
        try (Connection con = DBConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            
            pstmt.setString(1, loggedInUserId);
            pstmt.setString(2, "%@" + loggedInUserId + "%");
            pstmt.setString(3, loggedInUserId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    timelinePosts.add(new Post(
                        rs.getString("post_id"),
                        rs.getString("writer_id"),
                        rs.getString("content"),
                        rs.getInt("num_of_likes"),
                        rs.getInt("cmt_count"),  
                        rs.getInt("rep_count"),  
                        rs.getTimestamp("created_at"),
                        rs.getString("original_post_id"),
                        rs.getString("original_writer_id"),
                        rs.getString("original_content")
                    ));
                }
            }
        }
        return timelinePosts;
    }

    public List<Post> getUserBoard(String userIdToView) throws SQLException {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT p.post_id, p.writer_id, p.content, p.num_of_likes, p.created_at, " +
                     "       p.original_post_id, op.writer_id AS original_writer_id, op.content AS original_content, " +
                     "       (SELECT COUNT(*) FROM comment c WHERE c.post_id = p.post_id) AS cmt_count, " +
                     "       (SELECT COUNT(*) FROM posts r WHERE r.original_post_id = p.post_id) AS rep_count " +
                     "FROM posts AS p " +
                     "LEFT JOIN posts AS op ON p.original_post_id = op.post_id " +
                     "WHERE p.writer_id = ? OR p.content LIKE ? " + 
                     "ORDER BY p.created_at DESC LIMIT 20";
        
        try (Connection con = DBConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, userIdToView);
            pstmt.setString(2, "%@" + userIdToView + "%");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    posts.add(new Post(
                        rs.getString("post_id"),
                        rs.getString("writer_id"),
                        rs.getString("content"),
                        rs.getInt("num_of_likes"),
                        rs.getInt("cmt_count"),
                        rs.getInt("rep_count"),
                        rs.getTimestamp("created_at"),
                        rs.getString("original_post_id"),
                        rs.getString("original_writer_id"),
                        rs.getString("original_content")
                    ));
                }
            }
        }
        return posts;
    }

    public boolean writePost(String loggedInUserId, String content) throws SQLException {
        String postId = "p_" + UUID.randomUUID().toString().substring(0, 8);
        String sql = "insert into posts (post_id, content, writer_id, num_of_likes) values (?, ?, ?, 0)";
        
        try (Connection con = DBConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            pstmt.setString(2, content);
            pstmt.setString(3, loggedInUserId);
            if (pstmt.executeUpdate() > 0) {
                parseAndLinkHashtags(con, postId, content);
                return true;
            }
        }
        return false;
    }
    
    public boolean writeRepost(String loggedInUserId, String postIdToLink, String quoteContent) throws SQLException {
        String checkSql = "select writer_id from posts where post_id = ?";
        try (Connection con = DBConnector.getConnection();
             PreparedStatement checkPstmt = con.prepareStatement(checkSql)) {
            checkPstmt.setString(1, postIdToLink);
             try (ResultSet rs = checkPstmt.executeQuery()) {
                 if (!rs.next()) return false;
             }
        }

        String newPostId = "p_" + UUID.randomUUID().toString().substring(0, 8);
        String sql;
        if (quoteContent == null || quoteContent.isEmpty()) {
             sql = "insert into posts (post_id, writer_id, original_post_id, num_of_likes) values (?, ?, ?, 0)";
        } else {
            sql = "insert into posts (post_id, content, writer_id, original_post_id, num_of_likes) values (?, ?, ?, ?, 0)";
        }
        
        try (Connection con = DBConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, newPostId);
            if (quoteContent == null || quoteContent.isEmpty()) {
                 pstmt.setString(2, loggedInUserId);
                 pstmt.setString(3, postIdToLink);
            } else {
                pstmt.setString(2, quoteContent);
                pstmt.setString(3, loggedInUserId);
                pstmt.setString(4, postIdToLink);
            }
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deletePost(String postId, String currentUserId) throws SQLException {
        String checkSql = "SELECT writer_id FROM posts WHERE post_id = ?";
        try (Connection con = DBConnector.getConnection();
             PreparedStatement checkPstmt = con.prepareStatement(checkSql)) {
            checkPstmt.setString(1, postId);
            try (ResultSet rs = checkPstmt.executeQuery()) {
                if (rs.next()) {
                    if (!rs.getString("writer_id").equals(currentUserId)) return false;
                } else {
                    return false;
                }
            }
            String delSql = "DELETE FROM posts WHERE post_id = ?";
            try (PreparedStatement delPstmt = con.prepareStatement(delSql)) {
                delPstmt.setString(1, postId);
                return delPstmt.executeUpdate() > 0;
            }
        }
    }

    // --- 3. Social ---
    public boolean checkFollowStatus(String loggedInUserId, String userToView) throws SQLException {
        String sql = "select 1 from follow_relationship where user_id = ? and following_id = ?";
         try (Connection con = DBConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, loggedInUserId);
            pstmt.setString(2, userToView);
            try (ResultSet rs = pstmt.executeQuery()) { return rs.next(); }
        }
    }

    public boolean followUser(String loggedInUserId, String userToFollow) throws SQLException {
        if (loggedInUserId.equals(userToFollow)) return false;
        String sql = "insert into follow_relationship (user_id, following_id) values (?, ?)";
        try (Connection con = DBConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, loggedInUserId);
            pstmt.setString(2, userToFollow);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || e.getErrorCode() == 1452) return false;
            throw e;
        }
    }

    public boolean unfollowUser(String loggedInUserId, String userToUnfollow) throws SQLException {
        String sql = "delete from follow_relationship where user_id = ? and following_id = ?";
        try (Connection con = DBConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, loggedInUserId);
            pstmt.setString(2, userToUnfollow);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public List<String> getFollowingList(String userId) throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "select following_id from follow_relationship where user_id = ?";
        try (Connection con = DBConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(rs.getString("following_id"));
            }
        }
        return list;
    }
    
    public List<String> getFollowerList(String userId) throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "select user_id from follow_relationship where following_id = ?";
        try (Connection con = DBConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(rs.getString("user_id"));
            }
        }
        return list;
    }

    // --- 4. React ---
    public boolean checkPostLikeStatus(String loggedInUserId, String postId) throws SQLException {
        String sql = "select 1 from post_like where liker_id = ? and post_id = ?";
         try (Connection con = DBConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, loggedInUserId);
            pstmt.setString(2, postId);
            try (ResultSet rs = pstmt.executeQuery()) { return rs.next(); }
        }
    }
    
    public boolean likePost(String loggedInUserId, String postId) throws SQLException {
        String insertSql = "insert into post_like (liker_id, post_id) values (?, ?)";
        String updateSql = "update posts set num_of_likes = num_of_likes + 1 where post_id = ?";
        
        try (Connection con = DBConnector.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement insertPstmt = con.prepareStatement(insertSql)) {
                insertPstmt.setString(1, loggedInUserId);
                insertPstmt.setString(2, postId);
                insertPstmt.executeUpdate();
            }
            try (PreparedStatement updatePstmt = con.prepareStatement(updateSql)) {
                updatePstmt.setString(1, postId);
                updatePstmt.executeUpdate();
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || e.getErrorCode() == 1452) return false;
            throw e;
        }
    }

    public boolean unlikePost(String loggedInUserId, String postId) throws SQLException {
        String deleteSql = "delete from post_like where liker_id = ? and post_id = ?";
        String updateSql = "update posts set num_of_likes = num_of_likes - 1 where post_id = ? and num_of_likes > 0";
        
        try (Connection con = DBConnector.getConnection()) {
            con.setAutoCommit(false);
            int affectedRows = 0;
            try (PreparedStatement pstmtDelete = con.prepareStatement(deleteSql)) {
                pstmtDelete.setString(1, loggedInUserId);
                pstmtDelete.setString(2, postId);
                affectedRows = pstmtDelete.executeUpdate();
            }
            if (affectedRows == 0) {
                 con.rollback(); return false;
            }
            try (PreparedStatement pstmtUpdate = con.prepareStatement(updateSql)) {
                pstmtUpdate.setString(1, postId);
                pstmtUpdate.executeUpdate();
            }
            con.commit();
            return true;
        }
    }
    
    public List<Comment> getComments(String postId) throws SQLException {
        List<Comment> orderedComments = new ArrayList<>();
        
        String parentSql = "SELECT comment_id, parent_comment_id, writer_id, content, num_of_likes, created_at " +
                           "FROM comment " +
                           "WHERE post_id = ? AND parent_comment_id IS NULL " +
                           "ORDER BY created_at ASC";

        String childSql = "SELECT comment_id, parent_comment_id, writer_id, content, num_of_likes, created_at " +
                          "FROM comment " +
                          "WHERE parent_comment_id = ? " +
                          "ORDER BY created_at ASC";

        try (Connection con = DBConnector.getConnection();
             PreparedStatement parentPstmt = con.prepareStatement(parentSql);
             PreparedStatement childPstmt = con.prepareStatement(childSql)) {

            parentPstmt.setString(1, postId);
            try (ResultSet parentRs = parentPstmt.executeQuery()) {
                while (parentRs.next()) {
                    Comment parent = new Comment(
                        parentRs.getString("comment_id"),
                        parentRs.getString("parent_comment_id"),
                        parentRs.getString("writer_id"),
                        parentRs.getString("content"),
                        parentRs.getInt("num_of_likes"),
                        parentRs.getTimestamp("created_at")
                    );
                    orderedComments.add(parent);

                    childPstmt.setString(1, parent.getCommentId());
                    try (ResultSet childRs = childPstmt.executeQuery()) {
                        while (childRs.next()) {
                            Comment child = new Comment(
                                childRs.getString("comment_id"),
                                childRs.getString("parent_comment_id"),
                                childRs.getString("writer_id"),
                                childRs.getString("content"),
                                childRs.getInt("num_of_likes"),
                                childRs.getTimestamp("created_at")
                            );
                            orderedComments.add(child);
                        }
                    }
                }
            }
        }
        return orderedComments;
    }
    public boolean writeComment(String loggedInUserId, String postId, String content) throws SQLException {
        String commentId = "c_" + UUID.randomUUID().toString().substring(0, 8);
        String sql = "insert into comment (comment_id, parent_comment_id, content, writer_id, post_id, num_of_likes) values (?, null, ?, ?, ?, 0)";
        try (Connection con = DBConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, commentId);
            pstmt.setString(2, content);
            pstmt.setString(3, loggedInUserId);
            pstmt.setString(4, postId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1452) return false;
            throw e;
        }
    }
    
    public boolean writeReply(String loggedInUserId, String postId, String parentCommentId, String content) throws SQLException {
        String replyId = "c_" + UUID.randomUUID().toString().substring(0, 8);
        String sql = "insert into comment (comment_id, parent_comment_id, content, writer_id, post_id, num_of_likes) values (?, ?, ?, ?, ?, 0)";
        try (Connection con = DBConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, replyId);
            pstmt.setString(2, parentCommentId);
            pstmt.setString(3, content);
            pstmt.setString(4, loggedInUserId);
            pstmt.setString(5, postId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1452) return false;
            throw e;
        }
    }

    public boolean deleteComment(String commentId, String currentUserId) throws SQLException {
        String checkSql = "SELECT writer_id FROM comment WHERE comment_id = ?";
        try (Connection con = DBConnector.getConnection();
             PreparedStatement checkPstmt = con.prepareStatement(checkSql)) {
            checkPstmt.setString(1, commentId);
            try (ResultSet rs = checkPstmt.executeQuery()) {
                if (rs.next()) {
                    if (!rs.getString("writer_id").equals(currentUserId)) return false;
                } else {
                    return false;
                }
            }
            String delSql = "DELETE FROM comment WHERE comment_id = ?";
            try (PreparedStatement delPstmt = con.prepareStatement(delSql)) {
                delPstmt.setString(1, commentId);
                return delPstmt.executeUpdate() > 0;
            }
        }
    }

    public boolean checkCommentLikeStatus(String loggedInUserId, String commentId) throws SQLException {
        String sql = "select 1 from comment_like where liker_id = ? and comment_id = ?";
         try (Connection con = DBConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, loggedInUserId);
            pstmt.setString(2, commentId);
            try (ResultSet rs = pstmt.executeQuery()) { return rs.next(); }
        }
    }

    public boolean likeComment(String loggedInUserId, String commentId) throws SQLException {
        String insertSql = "insert into comment_like (liker_id, comment_id) values (?, ?)";
        String updateSql = "update comment set num_of_likes = num_of_likes + 1 where comment_id = ?";
        try (Connection con = DBConnector.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement insertPstmt = con.prepareStatement(insertSql)) {
                insertPstmt.setString(1, loggedInUserId);
                insertPstmt.setString(2, commentId);
                insertPstmt.executeUpdate();
            }
            try (PreparedStatement updatePstmt = con.prepareStatement(updateSql)) {
                updatePstmt.setString(1, commentId);
                updatePstmt.executeUpdate();
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || e.getErrorCode() == 1452) return false;
            throw e;
        }
    }

    public boolean unlikeComment(String loggedInUserId, String commentId) throws SQLException {
        String deleteSql = "delete from comment_like where liker_id = ? and comment_id = ?";
        String updateSql = "update comment set num_of_likes = num_of_likes - 1 where comment_id = ? and num_of_likes > 0";
        try (Connection con = DBConnector.getConnection()) {
            con.setAutoCommit(false);
            int affectedRows = 0;
            try (PreparedStatement pstmtDelete = con.prepareStatement(deleteSql)) {
                pstmtDelete.setString(1, loggedInUserId);
                pstmtDelete.setString(2, commentId);
                affectedRows = pstmtDelete.executeUpdate();
            }
            if (affectedRows == 0) { con.rollback(); return false; }
            try (PreparedStatement pstmtUpdate = con.prepareStatement(updateSql)) {
                pstmtUpdate.setString(1, commentId);
                pstmtUpdate.executeUpdate();
            }
            con.commit();
            return true;
        }
    }

    // --- 5. Explore ---
    
    public List<Post> searchByHashtag(String tagText) throws SQLException {
        List<Post> posts = new ArrayList<>();
        String sql = "select p.post_id, p.writer_id, p.content, p.num_of_likes, p.created_at, " +
                     "       p.original_post_id, op.writer_id AS original_writer_id, op.content AS original_content, " +
                     "       (SELECT COUNT(*) FROM comment c WHERE c.post_id = p.post_id) AS cmt_count, " +
                     "       (SELECT COUNT(*) FROM posts r WHERE r.original_post_id = p.post_id) AS rep_count " +
                     "from posts as p " +
                     "LEFT JOIN posts AS op ON p.original_post_id = op.post_id " +
                     "join post_hashtag as ph on p.post_id = ph.post_id " +
                     "join hashtag as h on ph.hashtag_id = h.hashtag_id " +
                     "where h.tag_text = ? " +
                     "order by p.created_at desc limit 20";
        
        try (Connection con = DBConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, tagText);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    posts.add(new Post(
                        rs.getString("post_id"),
                        rs.getString("writer_id"),
                        rs.getString("content"),
                        rs.getInt("num_of_likes"),
                        rs.getInt("cmt_count"),
                        rs.getInt("rep_count"),
                        rs.getTimestamp("created_at"),
                        rs.getString("original_post_id"),
                        rs.getString("original_writer_id"),
                        rs.getString("original_content")
                    ));
                }
            }
        }
        return posts;
    }

    // --- 6. Util ---
    public void parseAndLinkHashtags(Connection con, String postId, String content) {
        if (content == null) return;
        Pattern pattern = Pattern.compile("#([a-zA-Z0-9_ㄱ-ㅎㅏ-ㅣ가-힣]+)");
        Matcher matcher = pattern.matcher(content);
        
        String insertTagSql = "INSERT IGNORE INTO hashtag (tag_text) VALUES (?)";
        String selectTagSql = "SELECT hashtag_id FROM hashtag WHERE tag_text = ?";
        String linkSql = "INSERT IGNORE INTO post_hashtag (post_id, hashtag_id) VALUES (?, ?)";

        try {
            while (matcher.find()) {
                String tagText = matcher.group(1);
                int hashtagId = -1;
                try (PreparedStatement insertPstmt = con.prepareStatement(insertTagSql)) {
                    insertPstmt.setString(1, tagText);
                    insertPstmt.executeUpdate();
                }
                try (PreparedStatement selectPstmt = con.prepareStatement(selectTagSql)) {
                    selectPstmt.setString(1, tagText);
                    try (ResultSet rs = selectPstmt.executeQuery()) {
                        if (rs.next()) hashtagId = rs.getInt("hashtag_id");
                    }
                }
                if (hashtagId != -1) {
                    try (PreparedStatement linkPstmt = con.prepareStatement(linkSql)) {
                        linkPstmt.setString(1, postId);
                        linkPstmt.setInt(2, hashtagId);
                        linkPstmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("DB error during parsing hashtags: " + e.getMessage());
        }
    }

    public boolean checkUserExists(String userId) throws SQLException {
        String checkSql = "select user_id from user where user_id = ?";
        try (Connection con = DBConnector.getConnection();
             PreparedStatement checkPstmt = con.prepareStatement(checkSql)) {
            checkPstmt.setString(1, userId);
            try (ResultSet rs = checkPstmt.executeQuery()) { return rs.next(); }
        }
    }
}
