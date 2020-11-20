package sample;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.BoundingBox;
import javafx.scene.Cursor;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Controller {
    @FXML
    private ImageView closeIcon;
    @FXML
    private ImageView largeCover;
    @FXML
    private HBox infoPanel;
    @FXML
    private TextArea description;
    @FXML
    private ImageView loadingGif;
    @FXML
    private TextField searchField;
    @FXML
    private FlowPane flowPane;
    private String prevSearchTerm;
    private Timeline searchDelay = new Timeline(
        new KeyFrame(Duration.seconds(1),
           event -> searchMovie()));

    private Runnable searchRun = () -> {
        loadingGif.setVisible(true);
        List<CoverImage> coverList = new ArrayList();
        List<TVDBResult> hits = AlgoliaAPI.getSeriesHits(prevSearchTerm, "", 30);
        int actualHits = 0;
        for (TVDBResult result : hits)
            if (isValid(result)) {
                actualHits++;
                Platform.runLater(() -> coverList.add(addCoverElement(result)));
            }
        if (actualHits > 0) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(actualHits);
            while (coverList.size() < actualHits) {
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            for (CoverImage covimg : coverList)
                executor.execute(covimg.fetchImage);
            executor.shutdown();
        }
        Platform.runLater(() -> loadingGif.setVisible(false));
        return;
    };

    @FXML
    protected void initialize() {
        prevSearchTerm = "";
        loadingGif.setVisible(false);
        infoPanel.setVisible(false);
        infoPanel.setOnMouseClicked(me -> {
            System.out.printf("       x: %s y: %s\nscreen x: %s y: %s\n scene x: %s y: %s\n cover x: %s y: %s\n", me.getX(), me.getY(), me.getScreenX(), me.getScreenY(), me.getSceneX(), me.getSceneY(), largeCover.getBoundsInParent().getMaxX(), largeCover.getBoundsInParent().getMaxY());
            BoundingBox bb = new BoundingBox(me.getX(), me.getY(), 1, 1);
            System.out.println(bb);
            System.out.println(largeCover.getBoundsInParent());
            if (largeCover.getBoundsInParent().intersects(bb))
                System.out.println("OwO");
        });
    }

    public void checkSearchTimer() {
        loadingGif.setVisible(true);
        searchDelay.stop();
        searchDelay.play();
    }

    private void searchMovie() {
        String searchTerm = searchField.getText();
        if (!prevSearchTerm.equals(searchTerm) && searchTerm.length() > 1) {
            prevSearchTerm = searchTerm;
            flowPane.getChildren().clear();
            Thread t = new Thread(searchRun);
            t.setDaemon(true);
            t.start();
        } else
            loadingGif.setVisible(false);
    }

    private boolean isValid(TVDBResult result) {
        return (result.getImage() != null &&
                result.getImage().length() >= 1 &&
                !result.getImage().equals("https://artworks.thetvdb.com/banners/images/missing/movie.jpg") &&
                !result.getImage().equals("https://artworks.thetvdb.com/banners/images/missing/series.jpg") &&
                result.getOverviews() != null &&
                result.getOverviews().containsKey("eng"));
    }

    private String buildInfo(TVDBResult result) {
        String year = "";
        if (result.getReleased() != null) {
            if (!result.getName().contains(result.getReleased()))
                year = String.format("(%s)", result.getReleased());
        } else if (result.getFirstAired() != null) {
            if (!result.getName().contains(result.getFirstAired()))
                year = String.format("(%s)", result.getFirstAired());
        }
        return String.format("%s %s\n\n%s",result.getName(), year,result.getOverviews().get("eng"));
    }

    private void closeInfoPanel() {
        System.out.println();
        infoPanel.setVisible(false);
        flowPane.setEffect(null);
        searchField.setEffect(null);
    }

    private CoverImage addCoverElement(TVDBResult result) {
        CoverImage imgView = new CoverImage(new Image("placeholder.png"), result);
        imgView.setPreserveRatio(true);
        imgView.setFitWidth(250);
        imgView.setEffect(new DropShadow());
        imgView.setCursor(Cursor.HAND);

        ScaleTransition scaleUpTransition = new ScaleTransition();
        scaleUpTransition.setDuration(Duration.millis(100));
        scaleUpTransition.setNode(imgView);
        scaleUpTransition.setToX(1.05);
        scaleUpTransition.setToY(1.05);

        ScaleTransition scaleDownTransition = new ScaleTransition();
        scaleDownTransition.setDuration(Duration.millis(100));
        scaleDownTransition.setNode(imgView);
        scaleDownTransition.setToX(1);
        scaleDownTransition.setToY(1);

        imgView.setOnMouseClicked(mouseEvent -> {
            if (imgView.getInfo().getOverviews() != null && imgView.getInfo().getOverviews().containsKey("eng")) {
                description.setText(buildInfo(imgView.getInfo()));
                largeCover.setImage(imgView.getImage());
                flowPane.setEffect(new GaussianBlur(25));
                searchField.setEffect(new GaussianBlur(25));
                infoPanel.setVisible(true);
            }
        });

        imgView.setOnMouseEntered(MouseEvent -> scaleUpTransition.play());
        imgView.setOnMouseExited(MouseEvent -> scaleDownTransition.play());
        flowPane.getChildren().add(imgView);
        return imgView;
    }
}

