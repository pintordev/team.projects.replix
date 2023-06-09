package org.example.controller;

import org.example.Container;
import org.example.dto.Review;
import org.example.service.ReviewService;
import org.example.util.DBUtil;
import org.example.util.SecSql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewController {

    private ReviewService reviewService;

    public ReviewController() {
        this.reviewService = new ReviewService();
    }

    public void pagedList() {
        if (Container.session.getSessionState() != 3 && Container.session.getSessionState() != 4) return;

        SecSql sql = new SecSql();

        sql.append("SELECT A.`id`, A.`comment`, A.`score`, A.`replayFlag`, A.regDate, A.updateDate, B.`name` AS 'userName', C.`name` AS 'contentName'");
        sql.append("FROM `review` as A");
        sql.append("INNER JOIN `user` as B");
        sql.append("ON A.`userId` = B.`id`");
        sql.append("INNER JOIN `content` as C");
        sql.append("ON A.`contentId` = C.`id`");
        sql.append("WHERE A.`contentId` = ?", Container.session.getSessionContent().getId());
        sql.append("ORDER BY regDate DESC");
        sql.append("LIMIT ?, ?", (Container.session.getSessionReviewPage() - 1) * Container.session.getSessionReviewItemsPerPage(), Container.session.getSessionReviewItemsPerPage());

        List<Map<String, Object>> reviewMapList = DBUtil.selectRows(Container.connection, sql);

        if (reviewMapList.isEmpty()) {
            System.out.printf("\"%s\" 게시판에 남겨진 리뷰가 없습니다.\n", Container.session.getSessionContent().getName());
            System.out.println("리뷰를 남겨보세요.");
            return;
        }

        List<Review> reviewList = new ArrayList<>();
        for (Map<String, Object> reviewMap : reviewMapList) {
            reviewList.add(new Review(reviewMap));
        }

        System.out.println("https://replix.io/contents?id=%d&review?page=\n\n");
        Container.systemController.adComment();
        System.out.println("번호 / 글쓴이 / 내용 / 별점 / 재관람의사 / 작성시간");
        System.out.println("-".repeat(50));
        for (Review review : reviewList) {
            System.out.printf("%2d / %s / %s / %.1f / %s / %s\n", review.getId(), review.getUserName(), review.getComment(), review.getScore(), review.isReplayFlag() ? "있음" : "없음", review.getRegDate());
        }

        Container.session.goToReview();
    }

    public void post() {

        String score;
        String comment;
        String replayAnswer;
        int replayFlag = 0;

        while (true) {
            System.out.println("-".repeat(30));
            System.out.printf("\"%s\" 에 대한 별점을 입력해주세요. (0 ~ 5; 0.5점 단위) [입력 종료: \"q;\"]\n", Container.session.getSessionContent().getName());
            System.out.printf(">> ");
            score = Container.scanner.nextLine().trim();

            if (Container.systemController.isQuit(score)) return;

            if (score.length() == 0) {
                System.out.println("별점을 입력하지 않았습니다.\n");
                continue;
            }

            score = Double.parseDouble(score) + "";

            Map<String, Double> map = new HashMap<>();

            for (int i = 0; i <= 10; i++) {
                map.put(i * 0.5 + "", i * 0.5);
            }

            if (!map.containsKey(score)) {
                System.out.println("0.5점 단위로 입력해주세요.\n");
                continue;
            }

            System.out.println("");

            break;
        }

        while (true) {
            System.out.printf("\"%s\" 에 대한 솔직한 리뷰를 남겨주세요. [입력 종료: \"q;\"]\n", Container.session.getSessionContent().getName());
            System.out.printf(">> ");
            comment = Container.scanner.nextLine().trim();

            if (Container.systemController.isQuit(comment)) return;

            if (comment.length() == 0) {
                System.out.println("리뷰를 입력하지 않았습니다.\n");
                continue;
            }

            System.out.println("");

            break;
        }

        while (true) {
            System.out.println("재관람 의사를 남겨주세요. (Y/N) [입력 종료: \"q;\"]");
            System.out.printf(">> ");
            replayAnswer = Container.scanner.nextLine().trim().toLowerCase();

            if (Container.systemController.isQuit(replayAnswer)) return;

            if (!replayAnswer.equals("y") && !replayAnswer.equals("n")) {
                System.out.println("Y 또는 N 으로 입력해주세요.\n");
                continue;
            }

            if (replayAnswer.equals("y")) replayFlag = 1;

            System.out.println("");

            break;
        }

        System.out.println("작성하신 리뷰가 등록되었습니다.");
        System.out.println("소중한 리뷰 감사합니다.");

        this.reviewService.post(comment, score, replayFlag);

        Container.contentController.detail();
    }

    public void delete() {
        List<Review> reviewList = this.reviewService.reviewByUserIdContentId();

        System.out.println("https://replix.io/contents?id=%d&review?page=\n\n");
        Container.systemController.adComment();
        System.out.printf("\n\"%s\"님이 남기신 리뷰...\n\n", Container.session.getSessionUser().getName());
        System.out.println("번호 / 내용 / 별점 / 재관람의사 / 작성시간");
        System.out.println("-".repeat(50));
        for (Review review : reviewList) {
            System.out.printf("%2d / %s / %.1f / %s / %s\n", review.getId(), review.getComment(), review.getScore(), review.isReplayFlag() ? "있음" : "없음", review.getRegDate());
        }

        System.out.println("-".repeat(50));
        System.out.println("삭제할 리뷰 번호를 입력해주세요");
        System.out.printf(">> ");

        int deleteId = Container.scanner.nextInt();
        Container.scanner.nextLine();

        boolean isInReviewList = false;

        for (Review review : reviewList) {
            if (review.getId() == deleteId) {
                isInReviewList = true;
            }
        }

        if (!isInReviewList) {
            System.out.println("삭제 가능한 리뷰 번호가 아닙니다.");
            pagedList();
            return;
        }

        this.reviewService.deleteById(deleteId);

        System.out.printf("%s님이 남기신 %d번째 리뷰가 삭제되었습니다.\n", Container.session.getSessionUser().getName(), deleteId);
        Container.contentController.detail();
    }

    //리뷰 수정
//    public void modify() {
//
//
//        // 지금까지 작상한 리뷰들을 불러온다.
//        //선택한다 수정한다.
//
//
//        SecSql sql = new SecSql();
//
//        sql.append("select *");
//        sql.append("from review");
//        sql.append("WHERE userId = 1 && contentId = 1");
//
//        List<Map<String, Object>> reviewMapList = DBUtil.selectRows(Container.connection, sql);
//        List<Review> reviewList = new ArrayList<>();
//        for (Map<String, Object> reviewMap : reviewMapList) {
//            reviewList.add(new Review(reviewMap));
//        }
//
//        System.out.println("번호 / 내용 / 별점 / 재관람의사");
//        System.out.println("=".repeat(50));
//        for (Review review : reviewList) {
//            System.out.printf("%d / %s / %.1f / %s\n", review.getId(), review.getComment(), review.getScore(), review.isReplayFlag() ? "있음" : "없음");
//        }
//
//        System.out.println("수정할 리뷰 번호를 입력해주세요");
//
//        int i = Container.scanner.nextInt();
//        Container.scanner.nextLine();
//
//        boolean isInReviewList = false;
//
//        Review selectedReview;
//
//        for (Review review : reviewList) {
//            if (review.getId() == i) {
//                isInReviewList = true;
//                selectedReview = review;
//            }
//        }
//
//        if (!isInReviewList) {
//            System.out.println("  수정 가능한 리뷰 번호가 아닙니다.");
//            return;
//        }
//
//        System.out.printf("  현재 별점: %.1f", selectedReview.getScore());
//        System.out.println("  별점을 수정하시겠습니까? (Y/N)");
//        System.out.printf("  >> ");
//        String answer = Container.scanner.nextLine().trim().toLowerCase();
//
//        String score = "";
//        if (answer.equals("y")) {
//            while (true) {
//                System.out.println("-".repeat(30));
//                System.out.printf("별점을 입력해주세요\n>>");
//                score = Container.scanner.nextLine().trim();
//
//                Map<String, Double> map = new HashMap<>();
//
//                for (int a = 0; a <= 10; a++) {
//                    map.put(a * 0.5 + "", a * 0.5);
//                }
//
//                if (!map.containsKey(score)) {
//                    System.out.println("0.5점 단위로 입력해주세요");
//                    continue;
//                }
//
//                break;
//            }
//        }
//
//        System.out.printf("  현재 내용: %s", selectedReview.getComment());
//        System.out.println("  내용을 수정하시겠습니까? (Y/N)");
//        System.out.printf("  >> ");
//        String answer = Container.scanner.nextLine().trim().toLowerCase();
//
//        String comment = "";
//        if (answer.equals("y")) {
//            System.out.println("  수정할 내용을 입력해주세요.");
//            System.out.printf("  >> ");
//            comment = Container.scanner.nextLine();
//        }
//
//        System.out.println("  현재 재관람 의사: %s", selectedReview.isReplayFlag() ? "있음" : "없음");
//        System.out.println("  내용을 수정하시겠습니까? (Y/N)");
//        System.out.println("  >>");
//        String answer = Container.scanner.nextLine().trim().toLowerCase();
//
//        String replayFlagAnswer = "";
//        if (replayFlagAnswer.equals("y")) {
//            System.out.println("  수정할 내용을 입력해주세요.");
//            System.out.println("  >>");
//            replayFlagAnswer = Container.scanner.nextLine();
//        }
//
//
//        System.out.println("재관람 의사\nY / N");
//        String replayFlagAnswer = Container.scanner.nextLine().trim().toLowerCase();
//        System.out.println(replayFlagAnswer);
//        int replayFlag = 0;
//        if (replayFlagAnswer.equals("y")) {
//            replayFlag = 1;
//            System.out.println("=재관람 의사가 있습니다.=\n작성하신 리뷰가 등록되었습니다.\n소중한 리뷰 감사합니다");
//        } else {
//            System.out.println("=재관람 의사가 없습니다.=\n작성하신 리뷰가 등록되었습니다.\n소중한 리뷰 감사합니다");
//        }
//
//
//        SecSql sql = new SecSql();
//
//        sql.append("UPDATE `review`");
//        sql.append("SET updateDate = NOW()");
//        sql.append(", `comment` = ?", comment);
//        sql.append(", `score` = ?", score);
//        sql.append(", `replayFlag` = ?", replayFlag);
//        sql.append("WHERE `userId` = ?", Container.session.getSessionUser().getId());
//        sql.append("&& `contentId` = ?", 1);
//
//        DBUtil.update(Container.connection, sql);
//    }

    public void prevPage() {
        if (Container.session.getSessionReviewPage() <= 1) return;
        Container.session.setSessionReviewPage(Container.session.getSessionReviewPage() - 1);
        pagedList();
    }

    public void nextPage() {
        if (!hasNextPage()) return;
        Container.session.setSessionReviewPage(Container.session.getSessionReviewPage() + 1);
        pagedList();
    }

    public boolean hasNextPage() {
        SecSql sql = new SecSql();

        sql.append("SELECT *");
        sql.append("FROM `review` as A");
        sql.append("WHERE A.`contentId` = ?", Container.session.getSessionContent().getId());
        sql.append("ORDER BY regDate DESC");
        sql.append("LIMIT ?, ?", Container.session.getSessionReviewPage() * Container.session.getSessionReviewItemsPerPage(), Container.session.getSessionReviewItemsPerPage());

        return DBUtil.selectRows(Container.connection, sql).size() != 0;
    }
}
