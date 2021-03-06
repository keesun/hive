package controllers;

import play.db.ebean.Model;

import models.resource.Resource;

import models.*;
import models.enumeration.Direction;
import models.enumeration.Operation;
import models.enumeration.ResourceType;

import play.data.Form;
import play.mvc.Call;
import play.mvc.Content;
import utils.AccessControl;

import play.mvc.Controller;
import play.mvc.Result;
import utils.Callback;
import utils.Constants;

import java.io.IOException;

/**
 * {@link BoardApp}과 {@link IssueApp}에서 공통으로 사용하는 기능을 담고 있는 컨트롤러 클래스
 */
public class AbstractPostingApp extends Controller {
    public static final int ITEMS_PER_PAGE = 15;

    /**
     * 검색 조건
     */
    public static class SearchCondition {
        public String orderBy;
        public String orderDir;
        public String filter;
        public int pageNum;

        /**
         * 기본 검색 조건으로 id 역순이며 1페이지를 보여준다.
         */
        public SearchCondition() {
            this.orderDir = Direction.DESC.direction();
            this.orderBy = "id";
            this.filter = "";
            this.pageNum = 1;
        }
    }

    /**
     * 새 댓글 저장 핸들러
     *
     * {@code commentForm}에서 입력값을 꺼내 현재 사용자를 작성자로 설정하고 댓글을 저장한다.
     * 현재 사용자 임시 저장소에 있는 첨부파일을 댓글의 첨부파일로 옮긴다.
     *
     * @param comment
     * @param commentForm
     * @param redirectTo
     * @param containerUpdater
     * @return
     * @throws IOException
     */
    public static Result newComment(Comment comment, Form<? extends Comment> commentForm, Call redirectTo, Callback containerUpdater) throws IOException {
        if (commentForm.hasErrors()) {
            flash(Constants.WARNING, "board.comment.empty");
            return redirect(redirectTo);
        }

        comment.setAuthor(UserApp.currentUser());
        containerUpdater.run(); // this updates comment.issue or comment.posting;
        comment.save();

        // Attach all of the files in the current user's temporary storage.
        Attachment.moveAll(UserApp.currentUser().asResource(), comment.asResource());

        return redirect(redirectTo);
    }

    /**
     * {@code target}을 삭제하고 {@code redirectTo}로 이동한다.
     *
     * when: 게시물이나 이슈 또는 그곳에 달린 댓글을 삭제할 때 사용한다.
     *
     * @param target
     * @param resource
     * @param redirectTo
     * @return
     */
    protected static Result delete(Model target, Resource resource, Call redirectTo) {
        if (!AccessControl.isAllowed(UserApp.currentUser(), resource, Operation.DELETE)) {
            return forbidden();
        }

        target.delete();

        return redirect(redirectTo);
    }

    /**
     * {@code posting}에 {@code original} 정보를 채우고 갱신한다.
     *
     * when: 게시물이나 이슈를 수정할 떄 사용한다.
     *
     * @param original
     * @param posting
     * @param postingForm
     * @param redirectTo
     * @param updatePosting
     * @return
     */
    protected static Result editPosting(AbstractPosting original, AbstractPosting posting, Form<? extends AbstractPosting> postingForm, Call redirectTo, Callback updatePosting) {
        if (postingForm.hasErrors()) {
            return badRequest(postingForm.errors().toString());
        }

        if (!AccessControl.isAllowed(UserApp.currentUser(), original.asResource(), Operation.UPDATE)) {
            return forbidden(views.html.error.forbidden.render(original.project));
        }

        posting.id = original.id;
        posting.createdDate = original.createdDate;
        posting.authorId = original.authorId;
        posting.authorLoginId = original.authorLoginId;
        posting.authorName = original.authorName;
        posting.project = original.project;
        updatePosting.run();
        posting.update();

        // Attach the files in the current user's temporary storage.
        Attachment.moveAll(UserApp.currentUser().asResource(), original.asResource());

        return redirect(redirectTo);
    }

    /**
     * 새 게시물 또는 이슈 생성 권한이 있는지 확인하고 {@code content}를 보여준다.
     *
     * when: 게시물이나 이슈 생성할 때 사용한다.
     *
     * @param project
     * @param resourceType
     * @param content
     * @return
     */
    public static Result newPostingForm(Project project, ResourceType resourceType, Content content) {
        if (!AccessControl.isProjectResourceCreatable(UserApp.currentUser(), project, resourceType)) {
            return forbidden(views.html.error.forbidden.render(project));
        }

        return ok(content);
    }

}
