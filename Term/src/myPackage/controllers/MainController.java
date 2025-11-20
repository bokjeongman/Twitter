package myPackage.controllers;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane; 
import myPackage.TwitApp;
import myPackage.TwitService;
import myPackage.models.Comment;
import myPackage.models.Post;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;



public class MainController {

    private TwitApp twitApp;
    private TwitService twitService;
    private String currentUserId;

    // --- [Common] ---
    @FXML private Label welcomeLabel;
    @FXML private Button logoutButton;

    // --- [Tab 1: Home] ---
    @FXML private TextArea postTextArea;
    @FXML private Button postButton;
    @FXML private Button refreshButton;
    @FXML private ListView<Post> timelineView;
    private ObservableList<Post> timelinePosts = FXCollections.observableArrayList();

    // --- [Tab 2: Post Details] ---
    @FXML private Label selectedPostLabel;
    @FXML private HBox postActionsBox;
    @FXML private Button likeButton;
    @FXML private Button unlikeButton;
    @FXML private Button repostButton;
    @FXML private Button quoteButton;
    @FXML private TextArea commentTextArea;
    @FXML private Button commentButton;
    @FXML private ListView<Comment> commentListView;
    private ObservableList<Comment> commentList = FXCollections.observableArrayList();
    @FXML private HBox commentActionsBox;
    @FXML private Button likeCommentButton;
    @FXML private Button unlikeCommentButton;
    @FXML private Button replyButton;
    
    private Post selectedPost; 
    private Comment selectedComment; 

    // --- [Tab 3: Explore] ---
    @FXML private TextField searchField;
    @FXML private Button searchTagButton;
    @FXML private Button searchUserButton;
    @FXML private ListView<Post> searchResultView;
    private ObservableList<Post> searchResultPosts = FXCollections.observableArrayList();

    // --- [Tab 4: Social] ---
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

    // --- [Tab 5: Account] ---
    @FXML private PasswordField newPasswordField;
    @FXML private Button changePasswordButton;
    @FXML private Label passwordStatusLabel;

    
    // --- [Methods] ---

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

    // [Tab 1] Load Timeline
    private void loadTimeline() {
        try {
            List<Post> posts = twitService.getTimeline(currentUserId);
            timelinePosts.setAll(posts);
        } catch (SQLException e) {
            showAlert("DB Error", "Error loading timeline: " + e.getMessage());
        }
    }

    // [Tab 1] Refresh
    @FXML
    protected void handleRefresh() {
        loadTimeline();
    }

    // [Tab 1] Write post
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
    
    // [Common] Log out
    @FXML
    protected void handleLogout() {
        twitApp.showLoginScreen();
    }

    // --- [Tab 2 logic: post] ---
    
    // click post
    private void showPostDetails(Post post) {
        if (post == null) {
            clearPostDetails();
            return;
        }

        selectedPost = post;
        selectedPostLabel.setText(post.toString());
        
        // activate HBox to like my post
        postActionsBox.setDisable(false); 
        
        // change button text by like status
        try {
            boolean alreadyLiked = twitService.checkPostLikeStatus(currentUserId, post.getPostId());
            likeButton.setDisable(alreadyLiked);
            unlikeButton.setDisable(!alreadyLiked);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // not activate repost or quote button when my post
        boolean isMyPost = post.getWriterId().equals(currentUserId);
        repostButton.setDisable(isMyPost);
        quoteButton.setDisable(isMyPost);
        
        // activate comment area
        commentTextArea.setDisable(false);
        commentButton.setDisable(false);
        
        loadComments(post.getPostId());
        clearCommentDetails();
    }

    // not select post 
    private void clearPostDetails() {
        selectedPost = null;
        postActionsBox.setDisable(true); // not activate again
        commentTextArea.setDisable(true);
        commentButton.setDisable(true);
        selectedPostLabel.setText("Select a post from the Home or Explore tab.");
        commentList.clear();
        clearCommentDetails();
    }

    // load comment
    private void loadComments(String postId) {
        try {
            List<Comment> comments = twitService.getComments(postId);
            commentList.setAll(comments);
        } catch (SQLException e) {
             showAlert("DB Error", "Error loading comments: " + e.getMessage());
        }
    }
    
    // like post
    @FXML
    protected void handleLikePost() {
        if (selectedPost == null) return;
        try {
            twitService.likePost(currentUserId, selectedPost.getPostId());
            loadTimeline();
            // reload detail info 
            Post updatedPost = findPostById(selectedPost.getPostId()); // find and update post in list
            showPostDetails(updatedPost != null ? updatedPost : selectedPost); 
        } catch (SQLException e) {
             showAlert("DB Error", "Error liking post: " + e.getMessage());
        }
    }
    
    // unlike post
    @FXML
    protected void handleUnlikePost() {
        if (selectedPost == null) return;
        try {
            twitService.unlikePost(currentUserId, selectedPost.getPostId());
            loadTimeline();
            Post updatedPost = findPostById(selectedPost.getPostId());
            showPostDetails(updatedPost != null ? updatedPost : selectedPost);
        } catch (SQLException e) {
             showAlert("DB Error", "Error unliking post: " + e.getMessage());
        }
    }
    
    // find specific post in timeline list to update num_of_likes
    private Post findPostById(String postId) {
        for (Post post : timelinePosts) {
            if (post.getPostId().equals(postId)) {
                return post;
            }
        }
        for (Post post : searchResultPosts) {
             if (post.getPostId().equals(postId)) {
                return post;
            }
        }
        return null;
    }
    
    // repost
    @FXML
    protected void handleRepost() {
        if (selectedPost == null) return;
        try {
            boolean success = twitService.writeRepost(currentUserId, selectedPost.getPostId(), null);
            if (success) {
                showAlert("Success", "Post reposted.");
                loadTimeline();
                clearPostDetails();
            } else {
                 showAlert("Failed", "Could not repost. (Cannot repost own post or post not found)");
            }
        } catch (SQLException e) {
            showAlert("DB Error", "Error during repost: " + e.getMessage());
        }
    }
    
    // quote
    @FXML
    protected void handleQuote() {
        if (selectedPost == null) return;
        String quoteContent = showTextInputDialog("Quote Post",
            "Quoting post from @" + selectedPost.getWriterId(), "Enter your quote content:");
        
        if (quoteContent != null && !quoteContent.isEmpty()) {
             try {
                boolean success = twitService.writeRepost(currentUserId, selectedPost.getPostId(), quoteContent);
                if (success) {
                    showAlert("Success", "Quote post created.");
                    loadTimeline();
                    clearPostDetails();
                } else {
                    showAlert("Failed", "Could not quote. (Cannot quote own post or post not found)");
                }
            } catch (SQLException e) {
                showAlert("DB Error", "Error during quote: " + e.getMessage());
            }
        }
    }

    // write comment
    @FXML
    protected void handleWriteComment() {
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

    // --- [Tab 2 logic: Comment] ---
    
    // click comment
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
    }

    // not select comment
    private void clearCommentDetails() {
        selectedComment = null;
        commentActionsBox.setDisable(true);
    }
    
    // like comment
    @FXML
    protected void handleLikeComment() {
        if (selectedComment == null) return;
        try {
            twitService.likeComment(currentUserId, selectedComment.getCommentId());
            loadComments(selectedPost.getPostId());
            clearCommentDetails();
        } catch (SQLException e) {
            showAlert("DB Error", "Error liking comment: " + e.getMessage());
        }
    }
    
    // unlike comment
    @FXML
    protected void handleUnlikeComment() {
        if (selectedComment == null) return;
        try {
            twitService.unlikeComment(currentUserId, selectedComment.getCommentId());
            loadComments(selectedPost.getPostId());
            clearCommentDetails();
        } catch (SQLException e) {
            showAlert("DB Error", "Error unliking comment: " + e.getMessage());
        }
    }

    // reply
    @FXML
    protected void handleReplyToComment() {
        if (selectedComment == null || selectedPost == null) return;
        
        if (selectedComment.getParentCommentId() != null) {
            showAlert("Notice", "You cannot reply to a reply.");
            return;
        }
        
        String replyContent = showTextInputDialog("Reply to Comment",
            "Replying to @" + selectedComment.getWriterId(), "Enter your reply:");
        
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
    
    // --- [Tab 3 logic: Explore] ---
    
    @FXML
    protected void handleSearchTag() {
        String tag = searchField.getText();
        if (tag.isEmpty()) return;
        
        try {
            List<Post> posts = twitService.searchByHashtag(tag);
            searchResultPosts.setAll(posts);
            if (posts.isEmpty()) {
                showAlert("Search Result", "No posts found with #" + tag);
            }
        } catch (SQLException e) {
            showAlert("DB Error", "Error searching hashtag: " + e.getMessage());
        }
    }
    
    @FXML
    protected void handleSearchUser() {
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

    // --- [Tab 4 logic: Social] ---
    
    @FXML
    protected void handleShowMyFollowing() {
        socialList.clear();
        socialList.add("--- [Users I Follow] ---");
        try {
            List<String> list = twitService.getFollowingList(currentUserId);
            if (list.isEmpty()) socialList.add("None");
            else socialList.addAll(list);
        } catch (SQLException e) {
            showAlert("DB Error", "Error loading following list: " + e.getMessage());
        }
    }

    @FXML
    protected void handleShowMyFollowers() {
        socialList.clear();
        socialList.add("--- [Users Who Follow Me] ---");
         try {
            List<String> list = twitService.getFollowerList(currentUserId);
            if (list.isEmpty()) socialList.add("None");
            else socialList.addAll(list);
        } catch (SQLException e) {
            showAlert("DB Error", "Error loading follower list: " + e.getMessage());
        }
    }
    
    @FXML
    protected void handleSocialSearchUser() {
        searchedUserId = socialSearchField.getText();
        if (searchedUserId.isEmpty()) {
            socialActionsBox.setDisable(true);
            return;
        }
        if (searchedUserId.equals(currentUserId)) {
            showAlert("Notice", "You cannot search for yourself here.");
            socialActionsBox.setDisable(true);
            return;
        }
        
        try {
            if (!twitService.checkUserExists(searchedUserId)) {
                showAlert("Search Failed", "User '" + searchedUserId + "' not found.");
                socialActionsBox.setDisable(true);
                return;
            }
            
            otherUserLabel.setText("Actions for [" + searchedUserId + "]:");
            socialActionsBox.setDisable(false);
            
            boolean alreadyFollowing = twitService.checkFollowStatus(currentUserId, searchedUserId);
            followButton.setDisable(alreadyFollowing);
            unfollowButton.setDisable(!alreadyFollowing);
            
        } catch (SQLException e) {
            showAlert("DB Error", "Error searching user: " + e.getMessage());
        }
    }
    
    @FXML
    protected void handleShowOtherFollowing() {
        if (searchedUserId == null) return;
        socialList.clear();
        socialList.add("--- [Users @" + searchedUserId + " Follows] ---");
        try {
            List<String> list = twitService.getFollowingList(searchedUserId);
            if (list.isEmpty()) socialList.add("None");
            else socialList.addAll(list);
        } catch (SQLException e) {
            showAlert("DB Error", "Error loading following list: " + e.getMessage());
        }
    }
    
    @FXML
    protected void handleShowOtherFollower() {
        if (searchedUserId == null) return;
        socialList.clear();
        socialList.add("--- [Followers of @" + searchedUserId + "] ---");
        try {
            List<String> list = twitService.getFollowerList(searchedUserId);
            if (list.isEmpty()) socialList.add("None");
            else socialList.addAll(list);
        } catch (SQLException e) {
            showAlert("DB Error", "Error loading follower list: " + e.getMessage());
        }
    }
    
    @FXML
    protected void handleFollow() {
        if (searchedUserId == null) return;
        try {
            twitService.followUser(currentUserId, searchedUserId);
            handleSocialSearchUser();
        } catch (SQLException e) {
            showAlert("DB Error", "Error during follow: " + e.getMessage());
        }
    }
    
    @FXML
    protected void handleUnfollow() {
        if (searchedUserId == null) return;
        try {
            twitService.unfollowUser(currentUserId, searchedUserId);
            handleSocialSearchUser();
        } catch (SQLException e) {
            showAlert("DB Error", "Error during unfollow: " + e.getMessage());
        }
    }
    
    // --- [Tab 5 logic: Account] ---
    
    @FXML
    protected void handleChangePassword() {
        String newPwd = newPasswordField.getText();
        if (newPwd.isEmpty()) {
            passwordStatusLabel.setText("New password cannot be empty.");
            return;
        }
        
        try {
            boolean success = twitService.changePassword(currentUserId, newPwd);
            if (success) {
                newPasswordField.clear();
                passwordStatusLabel.setText("Password changed successfully.");
            } else {
                 passwordStatusLabel.setText("Password change failed.");
            }
        } catch (SQLException e) {
             showAlert("DB Error", "Error changing password: " + e.getMessage());
        }
    }

    // --- [Util: alert and input] ---
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
           getClass().getResource("/resources/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("root");
        
        alert.showAndWait();
    }
    
    private String showTextInputDialog(String title, String header, String content) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(
           getClass().getResource("/resources/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("root");
        
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

} 