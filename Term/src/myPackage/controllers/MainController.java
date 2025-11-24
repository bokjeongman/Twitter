package myPackage.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import myPackage.TwitApp;
import myPackage.TwitService;
import myPackage.models.Comment;
import myPackage.models.Post;

import java.sql.SQLException;
import java.sql.Timestamp;
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

    @FXML
    public void initialize() {
        timelineView.setCellFactory(createPostCellFactory());
        searchResultView.setCellFactory(createPostCellFactory());
        socialListView.setCellFactory(createUserCellFactory());

        commentListView.setCellFactory(param -> new ListCell<Comment>() {
            private final VBox rootBox = new VBox(4); 
            private final Label infoLabel = new Label();
            private final Label contentLabel = new Label();
            private final Label likeLabel = new Label();

            {
                rootBox.setMaxWidth(Double.MAX_VALUE);
                rootBox.setAlignment(Pos.CENTER_LEFT);
                
                infoLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #8899A6; -fx-font-size: 12px;");
                contentLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                contentLabel.setWrapText(true);
                
                likeLabel.setStyle("-fx-text-fill: #E0245E; -fx-font-size: 12px; -fx-padding: 2 0 0 0;");
                
                rootBox.getChildren().addAll(infoLabel, contentLabel, likeLabel);
            }
            
            @Override
            protected void updateItem(Comment item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;"); 
                } else {
                    String timeStr = getRelativeTime(item.getCreatedAt());
                    
                    double leftPadding = 10; 
                    if (item.getParentCommentId() != null) {
                        leftPadding = 50; 
                        infoLabel.setText("↪ " + item.getWriterId() + "  •  " + timeStr);
                    } else {
                        leftPadding = 10;
                        infoLabel.setText("💬 " + item.getWriterId() + "  •  " + timeStr);
                    }
                    
                    rootBox.setPadding(new Insets(10, 10, 10, leftPadding));
                    contentLabel.setText(item.getContent());
                    
                    likeLabel.setText("❤️ " + item.getLikes());

                    if (isSelected()) {
                        rootBox.setStyle("-fx-background-color: #1C2732; -fx-border-color: #1DA1F2; -fx-border-width: 0 0 1 4;");
                    } else {
                        rootBox.setStyle("-fx-background-color: transparent; -fx-border-color: #38444D; -fx-border-width: 0 0 1 0;");
                    }
                    setGraphic(rootBox);
                    setText(null);
                }
            }
        });
    }

    private String getRelativeTime(Timestamp timestamp) {
        if (timestamp == null) return "";
        long diff = System.currentTimeMillis() - timestamp.getTime();
        long sec = diff / 1000;
        long min = sec / 60;
        long hour = min / 60;
        long day = hour / 24;

        if (sec < 60) return "Just now";
        if (min < 60) return min + "m";
        if (hour < 24) return hour + "h";
        if (day < 7) return day + "d";
        return timestamp.toString().substring(0, 10);
    }

    private String getColorForUser(String userId) {
        if (userId == null) return "#1DA1F2";
        int hash = userId.hashCode();
        String[] colors = {"#FFADAD", "#FFD6A5", "#FDFFB6", "#CAFFBF", "#9BF6FF", "#A0C4FF", "#BDB2FF", "#FFC6FF", "#1DA1F2", "#17BF63", "#FF9F1C", "#2EC4B6", "#E71D36", "#7209B7"};
        return colors[Math.abs(hash) % colors.length];
    }

    private Callback<ListView<Post>, ListCell<Post>> createPostCellFactory() {
        return param -> new ListCell<Post>() {
            private final VBox outerBox = new VBox();
            private final HBox cardBox = new HBox(12);
            private final StackPane profilePane = new StackPane();
            private final Circle profileCircle = new Circle(22); 
            private final Label profileInitial = new Label();

            private final VBox contentBox = new VBox(4);
            private final HBox headerBox = new HBox(5);
            private final Label nameLabel = new Label();      
            private final Label verifiedIcon = new Label("☑️"); 
            private final Label handleLabel = new Label();    
            private final Label timeLabel = new Label();      
            private final Label bodyLabel = new Label();      
            private final HBox footerBox = new HBox(20); 
            private final Label replyStat = new Label();
            private final Label retweetStat = new Label();
            private final Label likeStat = new Label();

            {
                outerBox.setFillWidth(true); 
                outerBox.setPadding(new Insets(0));

                cardBox.setPadding(new Insets(15, 20, 15, 20));
                cardBox.setAlignment(Pos.TOP_LEFT);
                cardBox.setMaxWidth(Double.MAX_VALUE); 
                cardBox.setStyle("-fx-background-color: #15202B; -fx-border-color: #38444D; -fx-border-width: 0 0 1 0;");

                profileInitial.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
                profilePane.getChildren().addAll(profileCircle, profileInitial);
                profilePane.setMinWidth(45); profilePane.setMinHeight(45);

                headerBox.setAlignment(Pos.BASELINE_LEFT);
                nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 15px;");
                verifiedIcon.setStyle("-fx-text-fill: #1DA1F2; -fx-font-size: 14px;");
                handleLabel.setStyle("-fx-text-fill: #71767B; -fx-font-size: 14px;");
                timeLabel.setStyle("-fx-text-fill: #71767B; -fx-font-size: 14px;");

                bodyLabel.setStyle("-fx-text-fill: #E7E9EA; -fx-font-size: 15px; -fx-line-spacing: 3px;");
                bodyLabel.setWrapText(true);

                replyStat.setStyle("-fx-text-fill: #71767B; -fx-font-size: 12px;");
                retweetStat.setStyle("-fx-text-fill: #71767B; -fx-font-size: 12px;");
                likeStat.setStyle("-fx-text-fill: #E0245E; -fx-font-size: 12px;");

                headerBox.getChildren().addAll(nameLabel, verifiedIcon, handleLabel, timeLabel);
                footerBox.getChildren().addAll(replyStat, retweetStat, likeStat);
                contentBox.getChildren().addAll(headerBox, bodyLabel, footerBox);
                cardBox.getChildren().addAll(profilePane, contentBox);
                HBox.setHgrow(contentBox, Priority.ALWAYS);
                
                outerBox.getChildren().add(cardBox);
            }

            @Override
            protected void updateItem(Post item, boolean empty) {
                super.updateItem(item, empty);
                
                if (getListView() != null) {
                    cardBox.prefWidthProperty().bind(getListView().widthProperty().subtract(20));
                }

                if (empty || item == null) {
                    setGraphic(null); setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    String writer = item.getWriterId();
                    profileCircle.setFill(Color.web(getColorForUser(writer)));
                    if (writer.length() > 0) profileInitial.setText(writer.substring(0, 1).toUpperCase());
                    
                    nameLabel.setText(writer);
                    handleLabel.setText("@" + writer);

                    if (writer.equals(currentUserId) || writer.equalsIgnoreCase("admin")) {
                        verifiedIcon.setVisible(true); verifiedIcon.setManaged(true);
                    } else {
                        verifiedIcon.setVisible(false); verifiedIcon.setManaged(false);
                    }

                    timeLabel.setText("• " + getRelativeTime(item.getCreatedAt()));
                    bodyLabel.setText(item.getContent());

                    likeStat.setText("❤️ " + item.getLikeCount());
                    retweetStat.setText("🔁 0"); 
                    replyStat.setText("💬 0"); 

                    if (isSelected()) {
                        cardBox.setStyle("-fx-background-color: #1C2732; -fx-border-color: #1DA1F2; -fx-border-width: 0 0 1 4;"); 
                    } else {
                        cardBox.setStyle("-fx-background-color: #15202B; -fx-border-color: #38444D; -fx-border-width: 0 0 1 0;");
                    }

                    setGraphic(outerBox); 
                    setText(null);
                    setPadding(Insets.EMPTY);
                }
            }
        };
    }

    private Callback<ListView<String>, ListCell<String>> createUserCellFactory() {
        return param -> new ListCell<String>() {
            private final HBox rootBox = new HBox(10);
            private final StackPane profilePane = new StackPane();
            private final Circle profileCircle = new Circle(18); 
            private final Label profileInitial = new Label();
            private final Label nameLabel = new Label();
            private final Label headerLabel = new Label();

            {
                rootBox.setPadding(new Insets(8, 12, 8, 12));
                rootBox.setAlignment(Pos.CENTER_LEFT);
                profileInitial.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
                profilePane.getChildren().addAll(profileCircle, profileInitial);
                nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
                headerLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #1DA1F2; -fx-font-weight: bold; -fx-padding: 5;");
                headerLabel.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null); setText(null);
                } else {
                    if (item.startsWith("---") || item.equals("None")) {
                        rootBox.getChildren().clear();
                        headerLabel.setText(item.replace("---", "").trim());
                        HBox headerBox = new HBox(headerLabel);
                        headerBox.setAlignment(Pos.CENTER);
                        setGraphic(headerBox);
                    } else {
                        rootBox.getChildren().clear();
                        profileCircle.setFill(Color.web(getColorForUser(item)));
                        if (item.length() > 0) profileInitial.setText(item.substring(0, 1).toUpperCase());
                        nameLabel.setText(item);
                        rootBox.getChildren().addAll(profilePane, nameLabel);
                        setGraphic(rootBox);
                    }
                    setText(null);
                }
            }
        };
    }

    public void setApp(TwitApp twitApp, TwitService twitService, String currentUserId) {
        this.twitApp = twitApp;
        this.twitService = twitService;
        this.currentUserId = currentUserId;
        welcomeLabel.setText("👋 " + currentUserId);
        
        timelineView.setItems(timelinePosts);
        commentListView.setItems(commentList);
        searchResultView.setItems(searchResultPosts);
        socialListView.setItems(socialList);

        timelineView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> showPostDetails(newV));
        searchResultView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> showPostDetails(newV));
        commentListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> showCommentDetails(newV));

        loadTimeline();
    }

    private void loadTimeline() {

        // 새로고침 버튼 잠깐 비활성화 (버튼이 있으면)
        if (refreshButton != null) {
            refreshButton.setDisable(true);
        }

        // DB에서 타임라인을 읽어오는 작업을 백그라운드 스레드에서 실행
        Task<List<Post>> task = new Task<List<Post>>() {
            @Override
            protected List<Post> call() throws Exception {
                return twitService.getTimeline(currentUserId);
            }
        };

        // 성공적으로 로딩이 끝났을 때 UI 업데이트
        task.setOnSucceeded(e -> {
            List<Post> posts = task.getValue();
            timelinePosts.setAll(posts);

            if (refreshButton != null) {
                refreshButton.setDisable(false);
            }
        });

        // 에러 났을 때 처리
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            showAlert("DB Error", "Error loading timeline: " + ex.getMessage());

            if (refreshButton != null) {
                refreshButton.setDisable(false);
            }
        });

        // 실제로 백그라운드에서 Task 실행
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
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

    private void showPostDetails(Post post) {
        if (post == null) {
            clearPostDetails();
            return;
        }
        selectedPost = post;

        VBox cardContainer = new VBox(15);
        cardContainer.setPadding(new Insets(25));
        cardContainer.setMaxWidth(Double.MAX_VALUE); 
        cardContainer.setStyle("-fx-background-color: #192734; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 3);");

        HBox headerBox = new HBox(12);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        StackPane profilePane = new StackPane();
        Circle profileCircle = new Circle(24);
        profileCircle.setFill(Color.web(getColorForUser(post.getWriterId())));
        Label profileInitial = new Label(post.getWriterId().substring(0, 1).toUpperCase());
        profileInitial.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        profilePane.getChildren().addAll(profileCircle, profileInitial);
        
        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(post.getWriterId());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: 900;");
        Label handleLabel = new Label("@" + post.getWriterId());
        handleLabel.setStyle("-fx-text-fill: #8899A6; -fx-font-size: 15px;");
        nameBox.getChildren().addAll(nameLabel, handleLabel);
        
        headerBox.getChildren().addAll(profilePane, nameBox);

        Label contentLabel = new Label(post.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-line-spacing: 5px;");

        Label dateLabel = new Label(post.getCreatedAt().toString());
        dateLabel.setStyle("-fx-text-fill: #8899A6; -fx-font-size: 15px;");
        dateLabel.setPadding(new Insets(10, 0, 10, 0));
        
        Separator separator = new Separator();
        separator.setOpacity(0.2);

        HBox statsBox = new HBox(20);
        Label likesLabel = new Label(post.getLikeCount() + " Likes");
        likesLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px;");
        
        Label repostsLabel = new Label("0 Reposts");
        repostsLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px;");
        statsBox.getChildren().addAll(likesLabel, repostsLabel);

        cardContainer.getChildren().addAll(headerBox, contentLabel, dateLabel, separator, statsBox);

        selectedPostLabel.setText(""); 
        selectedPostLabel.setGraphic(cardContainer);
        
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
        String currentId = selectedPost.getPostId(); 
        
        try {
            List<Post> updatedPosts = twitService.getTimeline(currentUserId);
            timelinePosts.setAll(updatedPosts); 
            
            Post updatedPost = null;
            for(Post p : timelinePosts) {
                if(p.getPostId().equals(currentId)) {
                    updatedPost = p;
                    break;
                }
            }
            
            if (updatedPost != null) {
                timelineView.getSelectionModel().select(updatedPost);
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
        selectedPostLabel.setText("👈 Select a post to view details.");
        selectedPostLabel.setGraphic(null); 
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
        ButtonType btnOk = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", btnOk, btnCancel);
        alert.setTitle("Delete Post");
        alert.setHeaderText("Are you sure you want to delete this post?");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());
        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent() && result.get() == btnOk) {
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
        ButtonType btnOk = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", btnOk, btnCancel);
        alert.setTitle("Delete Comment");
        alert.setHeaderText("Delete this comment?");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());
        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent() && result.get() == btnOk) {
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
        ButtonType btnOk = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION, "", btnOk, btnCancel);
        confirmAlert.setTitle("Delete Account");
        confirmAlert.setHeaderText("Warning!");
        confirmAlert.setContentText("Do you really want to delete account? This cannot be undone.");
        
        confirmAlert.getDialogPane().getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());
        confirmAlert.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        confirmAlert.getDialogPane().setMinWidth(400);

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == btnOk) {
            TextInputDialog pwdDialog = new TextInputDialog();
            pwdDialog.setTitle("Authentication");
            pwdDialog.setHeaderText("Enter password to confirm:");
            pwdDialog.setContentText("Password:");
            pwdDialog.getDialogPane().getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());

            Optional<String> pwdResult = pwdDialog.showAndWait();
            pwdResult.ifPresent(password -> {
                try {
                    boolean success = twitService.deleteAccount(currentUserId, password);
                    if (success) {
                        showAlert("Goodbye", "Account deleted successfully.");
                        twitApp.showLoginScreen(); 
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
