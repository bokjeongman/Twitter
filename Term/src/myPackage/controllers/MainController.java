package myPackage.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import myPackage.TwitApp;
import myPackage.TwitService;
import myPackage.models.Comment;
import myPackage.models.Post;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class MainController {

    private TwitApp twitApp;
    private TwitService twitService;
    private String currentUserId;

    @FXML private Label welcomeLabel;
    @FXML private Button logoutButton;

    @FXML private TextArea postTextArea;
    @FXML private Button postButton;
    @FXML private Button refreshButton;
    @FXML private ListView<Post> timelineView;
    private ObservableList<Post> timelinePosts = FXCollections.observableArrayList();

    @FXML private Label selectedPostLabel;
    @FXML private HBox postActionsBox;
    @FXML private Button likeButton;
    @FXML private Button unlikeButton;
    @FXML private Button repostButton;
    @FXML private Button quoteButton;
    @FXML private Button deletePostButton; 

    @FXML private TextArea commentTextArea;
    @FXML private Button commentButton;
    @FXML private ListView<Comment> commentListView;
    private ObservableList<Comment> commentList = FXCollections.observableArrayList();
    @FXML private HBox commentActionsBox;
    @FXML private Button likeCommentButton;
    @FXML private Button unlikeCommentButton;
    @FXML private Button replyButton;
    @FXML private Button deleteCommentButton; 
    
    private Post selectedPost; 
    private Comment selectedComment; 

    @FXML private TextField searchField;
    @FXML private Button searchTagButton;
    @FXML private Button searchUserButton;
    @FXML private ListView<Post> searchResultView;
    private ObservableList<Post> searchResultPosts = FXCollections.observableArrayList();

    @FXML private Button myFollowingButton;
    @FXML private Button myFollowerButton;
    @FXML private ListView<String> socialListView;
    private ObservableList<String> socialList = FXCollections.observableArrayList();
    @FXML private Label otherUserLabel;
    @FXML private TextField socialSearchField;
    @FXML private Button socialSearchButton;
    @FXML private FlowPane socialActionsBox; 
    @FXML private Button viewFollowingButton;
    @FXML private Button viewFollowerButton;
    @FXML private Button followButton;
    @FXML private Button unfollowButton;
    private String searchedUserId; 

    @FXML private PasswordField newPasswordField;
    @FXML private Button changePasswordButton;
    @FXML private Label passwordStatusLabel;

    public void setApp(TwitApp twitApp, TwitService twitService, String currentUserId) {
        this.twitApp = twitApp;
        this.twitService = twitService;
        this.currentUserId = currentUserId;

        welcomeLabel.setText(currentUserId);
        
        timelineView.setItems(timelinePosts);
        commentListView.setItems(commentList);
        searchResultView.setItems(searchResultPosts);
        socialListView.setItems(socialList);

        timelineView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldV, newV) -> showPostDetails(newV)
        );
         searchResultView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldV, newV) -> showPostDetails(newV)
        );
        commentListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldV, newV) -> showCommentDetails(newV)
        );

        loadTimeline();
    }

    private void loadTimeline() {
        try {
            List<Post> posts = twitService.getTimeline(currentUserId);
            timelinePosts.setAll(posts);
        } catch (SQLException e) {
            showAlert("DB Error", "Error loading timeline: " + e.getMessage());
        }
    }

    @FXML protected void handleRefresh() { loadTimeline(); }

    @FXML
    protected void handlePost() {
        String content = postTextArea.getText();
        if (content.isEmpty()) {
            showAlert("Input Error", "Post content cannot be empty.");
            return;
        }
        try {
            boolean success = twitService.writePost(currentUserId, content);
            if (success) {
                postTextArea.clear();
                loadTimeline();
            } else {
                showAlert("Post Failed", "Failed to create post.");
            }
        } catch (SQLException e) {
            showAlert("DB Error", "Error creating post: " + e.getMessage());
        }
    }
    
    @FXML protected void handleLogout() { twitApp.showLoginScreen(); }

    // --- [Tab 2: Post Details] ---
    private void showPostDetails(Post post) {
        if (post == null) {
            clearPostDetails();
            return;
        }
        selectedPost = post;
        selectedPostLabel.setText(post.toString());
        postActionsBox.setDisable(false);
        
        try {
            boolean alreadyLiked = twitService.checkPostLikeStatus(currentUserId, post.getPostId());
            likeButton.setDisable(alreadyLiked);
            unlikeButton.setDisable(!alreadyLiked);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        repostButton.setDisable(false);
        quoteButton.setDisable(false);
        
        // delete button activate when my post only
        boolean isMyPost = post.getWriterId().equals(currentUserId);
        if (deletePostButton != null) {
            deletePostButton.setVisible(isMyPost); 
            deletePostButton.setManaged(isMyPost);
        }

        commentTextArea.setDisable(false);
        commentButton.setDisable(false);
        loadComments(post.getPostId());
        clearCommentDetails();
    }
    
    private void refreshPostDetails() {
        if (selectedPost == null) return;
        try {
            List<Post> updatedPosts = twitService.getTimeline(currentUserId);
            timelinePosts.setAll(updatedPosts); 
            
            Post updatedPost = findPostById(selectedPost.getPostId());
            if (updatedPost != null) {
                showPostDetails(updatedPost); 
            } else {
                clearPostDetails();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void clearPostDetails() {
        selectedPost = null;
        postActionsBox.setDisable(true);
        if (deletePostButton != null) deletePostButton.setVisible(false); 
        commentTextArea.setDisable(true);
        commentButton.setDisable(true);
        selectedPostLabel.setText("Select a post from the Home or Explore tab.");
        commentList.clear();
        clearCommentDetails();
    }

    private void loadComments(String postId) {
        try {
            List<Comment> comments = twitService.getComments(postId);
            commentList.setAll(comments);
        } catch (SQLException e) {
             showAlert("DB Error", "Error loading comments: " + e.getMessage());
        }
    }
    
    @FXML protected void handleLikePost() {
        if (selectedPost == null) return;
        try {
            twitService.likePost(currentUserId, selectedPost.getPostId());
            refreshPostDetails();
        } catch (SQLException e) {
             showAlert("DB Error", "Error liking post: " + e.getMessage());
        }
    }
    
    @FXML protected void handleUnlikePost() {
        if (selectedPost == null) return;
        try {
            twitService.unlikePost(currentUserId, selectedPost.getPostId());
            refreshPostDetails();
        } catch (SQLException e) {
             showAlert("DB Error", "Error unliking post: " + e.getMessage());
        }
    }

    
    @FXML
    protected void handleDeletePost() {
        if (selectedPost == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Post");
        alert.setHeaderText("Are you sure you want to delete this post?");
        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = twitService.deletePost(selectedPost.getPostId(), currentUserId);
                if (success) {
                    showAlert("Success", "Post deleted.");
                    loadTimeline();
                    clearPostDetails();
                } else {
                    showAlert("Error", "Failed to delete post.");
                }
            } catch (SQLException e) {
                showAlert("DB Error", "Error deleting post: " + e.getMessage());
            }
        }
    }
    
    private Post findPostById(String postId) {
        for (Post post : timelinePosts) if (post.getPostId().equals(postId)) return post;
        for (Post post : searchResultPosts) if (post.getPostId().equals(postId)) return post;
        return null;
    }
    
    @FXML protected void handleRepost() {
        if (selectedPost == null) return;
        try {
            boolean success = twitService.writeRepost(currentUserId, selectedPost.getPostId(), null);
            if (success) {
                showAlert("Success", "Post reposted.");
                refreshPostDetails();
            } else {
                 showAlert("Failed", "Could not repost. (Cannot repost own post)");
            }
        } catch (SQLException e) {
            showAlert("DB Error", "Error during repost: " + e.getMessage());
        }
    }
    
    @FXML protected void handleQuote() {
        if (selectedPost == null) return;
        String quoteContent = showTextInputDialog("Quote Post", "Quoting post from @" + selectedPost.getWriterId(), "Enter your quote content:");
        if (quoteContent != null && !quoteContent.isEmpty()) {
             try {
                boolean success = twitService.writeRepost(currentUserId, selectedPost.getPostId(), quoteContent);
                if (success) {
                    showAlert("Success", "Quote post created.");
                    refreshPostDetails();
                } else {
                    showAlert("Failed", "Could not quote. (Cannot quote own post)");
                }
            } catch (SQLException e) {
                showAlert("DB Error", "Error during quote: " + e.getMessage());
            }
        }
    }

    @FXML protected void handleWriteComment() {
        if (selectedPost == null) return;
        String content = commentTextArea.getText();
        if (content.isEmpty()) {
            showAlert("Input Error", "Comment content cannot be empty.");
            return;
        }
        try {
            twitService.writeComment(currentUserId, selectedPost.getPostId(), content);
            commentTextArea.clear();
            loadComments(selectedPost.getPostId());
        } catch (SQLException e) {
             showAlert("DB Error", "Error writing comment: " + e.getMessage());
        }
    }

    private void showCommentDetails(Comment comment) {
        if (comment == null) {
            clearCommentDetails();
            return;
        }
        selectedComment = comment;
        commentActionsBox.setDisable(false);
        
        try {
             boolean alreadyLiked = twitService.checkCommentLikeStatus(currentUserId, comment.getCommentId());
             likeCommentButton.setDisable(alreadyLiked);
             unlikeCommentButton.setDisable(!alreadyLiked);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // activate delete button when my comment only
        boolean isMyComment = comment.getWriterId().equals(currentUserId);
        if (deleteCommentButton != null) {
            deleteCommentButton.setVisible(isMyComment);
            deleteCommentButton.setManaged(isMyComment);
        }
    }

    private void clearCommentDetails() {
        selectedComment = null;
        commentActionsBox.setDisable(true);
        if (deleteCommentButton != null) deleteCommentButton.setVisible(false); 
    }
    
    @FXML protected void handleLikeComment() {
        if (selectedComment == null) return;
        try {
            twitService.likeComment(currentUserId, selectedComment.getCommentId());
            loadComments(selectedPost.getPostId());
            clearCommentDetails();
        } catch (SQLException e) {
            showAlert("DB Error", "Error liking comment: " + e.getMessage());
        }
    }
    
    @FXML protected void handleUnlikeComment() {
        if (selectedComment == null) return;
        try {
            twitService.unlikeComment(currentUserId, selectedComment.getCommentId());
            loadComments(selectedPost.getPostId());
            clearCommentDetails();
        } catch (SQLException e) {
            showAlert("DB Error", "Error unliking comment: " + e.getMessage());
        }
    }

    @FXML protected void handleReplyToComment() {
        if (selectedComment == null || selectedPost == null) return;
        if (selectedComment.getParentCommentId() != null) {
            showAlert("Notice", "You cannot reply to a reply.");
            return;
        }
        String replyContent = showTextInputDialog("Reply to Comment", "Replying to @" + selectedComment.getWriterId(), "Enter your reply:");
        if (replyContent != null && !replyContent.isEmpty()) {
            try {
                twitService.writeReply(currentUserId, selectedPost.getPostId(), selectedComment.getCommentId(), replyContent);
                loadComments(selectedPost.getPostId());
                clearCommentDetails();
            } catch (SQLException e) {
                 showAlert("DB Error", "Error writing reply: " + e.getMessage());
            }
        }
    }

    @FXML
    protected void handleDeleteComment() {
        if (selectedComment == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Comment");
        alert.setHeaderText("Delete this comment?");
        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = twitService.deleteComment(selectedComment.getCommentId(), currentUserId);
                if (success) {
                    showAlert("Success", "Comment deleted.");
                    loadComments(selectedPost.getPostId());
                    clearCommentDetails();
                } else {
                    showAlert("Error", "Failed to delete comment.");
                }
            } catch (SQLException e) {
                showAlert("DB Error", "Error deleting comment: " + e.getMessage());
            }
        }
    }

    // --- [Explore] ---
    @FXML protected void handleSearchTag() {
        String tag = searchField.getText();
        if (tag.isEmpty()) return;
        try {
            List<Post> posts = twitService.searchByHashtag(tag);
            searchResultPosts.setAll(posts);
            if (posts.isEmpty()) showAlert("Search Result", "No posts found with #" + tag);
        } catch (SQLException e) {
            showAlert("DB Error", "Error searching hashtag: " + e.getMessage());
        }
    }
    
    @FXML protected void handleSearchUser() {
        String userId = searchField.getText();
        if (userId.isEmpty()) return;
        try {
            if (!twitService.checkUserExists(userId)) {
                 showAlert("Search Result", "User '" + userId + "' not found.");
                 searchResultPosts.clear();
                 return;
            }
            List<Post> posts = twitService.getUserBoard(userId);
            searchResultPosts.setAll(posts);
        } catch (SQLException e) {
             showAlert("DB Error", "Error searching user: " + e.getMessage());
        }
    }

    // --- [Social] ---
    @FXML protected void handleShowMyFollowing() {
        socialList.clear(); socialList.add("--- [Users I Follow] ---");
        try {
            List<String> list = twitService.getFollowingList(currentUserId);
            if (list.isEmpty()) socialList.add("None"); else socialList.addAll(list);
        } catch (SQLException e) { showAlert("DB Error", "Error: " + e.getMessage()); }
    }

    @FXML protected void handleShowMyFollowers() {
        socialList.clear(); socialList.add("--- [Users Who Follow Me] ---");
         try {
            List<String> list = twitService.getFollowerList(currentUserId);
            if (list.isEmpty()) socialList.add("None"); else socialList.addAll(list);
        } catch (SQLException e) { showAlert("DB Error", "Error: " + e.getMessage()); }
    }
    
    @FXML protected void handleSocialSearchUser() {
        searchedUserId = socialSearchField.getText();
        if (searchedUserId.isEmpty() || searchedUserId.equals(currentUserId)) {
            socialActionsBox.setDisable(true); return;
        }
        try {
            if (!twitService.checkUserExists(searchedUserId)) {
                showAlert("Failed", "User not found.");
                socialActionsBox.setDisable(true); return;
            }
            otherUserLabel.setText("Actions for [" + searchedUserId + "]:");
            socialActionsBox.setDisable(false);
            boolean following = twitService.checkFollowStatus(currentUserId, searchedUserId);
            followButton.setDisable(following);
            unfollowButton.setDisable(!following);
        } catch (SQLException e) { showAlert("DB Error", "Error: " + e.getMessage()); }
    }
    
    @FXML protected void handleShowOtherFollowing() {
        if (searchedUserId == null) return;
        socialList.clear(); socialList.add("--- [Users @" + searchedUserId + " Follows] ---");
        try {
            List<String> list = twitService.getFollowingList(searchedUserId);
            if (list.isEmpty()) socialList.add("None"); else socialList.addAll(list);
        } catch (SQLException e) { showAlert("DB Error", "Error: " + e.getMessage()); }
    }
    
    @FXML protected void handleShowOtherFollower() {
        if (searchedUserId == null) return;
        socialList.clear(); socialList.add("--- [Followers of @" + searchedUserId + "] ---");
        try {
            List<String> list = twitService.getFollowerList(searchedUserId);
            if (list.isEmpty()) socialList.add("None"); else socialList.addAll(list);
        } catch (SQLException e) { showAlert("DB Error", "Error: " + e.getMessage()); }
    }
    
    @FXML protected void handleFollow() {
        if (searchedUserId == null) return;
        try {
            twitService.followUser(currentUserId, searchedUserId);
            handleSocialSearchUser();
        } catch (SQLException e) { showAlert("DB Error", "Error: " + e.getMessage()); }
    }
    
    @FXML protected void handleUnfollow() {
        if (searchedUserId == null) return;
        try {
            twitService.unfollowUser(currentUserId, searchedUserId);
            handleSocialSearchUser();
        } catch (SQLException e) { showAlert("DB Error", "Error: " + e.getMessage()); }
    }
    
    // --- [Account] ---
    @FXML protected void handleChangePassword() {
        String newPwd = newPasswordField.getText();
        if (newPwd.isEmpty()) return;
        try {
            if (twitService.changePassword(currentUserId, newPwd)) {
                newPasswordField.clear();
                passwordStatusLabel.setText("Password changed successfully.");
            } else {
                 passwordStatusLabel.setText("Password change failed.");
            }
        } catch (SQLException e) { showAlert("DB Error", "Error: " + e.getMessage()); }
    }

    @FXML
    protected void handleDeleteAccount() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Account");
        confirmAlert.setHeaderText("Warning!");
        confirmAlert.setContentText("Do you really want to delete account? This cannot be undone.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            TextInputDialog pwdDialog = new TextInputDialog();
            pwdDialog.setTitle("Authentication");
            pwdDialog.setHeaderText("Enter your password to confirm deletion:");
            pwdDialog.setContentText("Password:");

            Optional<String> pwdResult = pwdDialog.showAndWait();
            pwdResult.ifPresent(password -> {
                try {
                    boolean success = twitService.deleteAccount(currentUserId, password);
                    if (success) {
                        showAlert("Goodbye", "Account deleted successfully.");
                        twitApp.showLoginScreen(); // 로그아웃
                    } else {
                        showAlert("Error", "Wrong password or deletion failed.");
                    }
                } catch (SQLException e) {
                    showAlert("DB Error", "Error deleting account: " + e.getMessage());
                }
            });
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("root");
        alert.showAndWait();
    }
    
    private String showTextInputDialog(String title, String header, String content) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title); dialog.setHeaderText(header); dialog.setContentText(content);
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("root");
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }
}
