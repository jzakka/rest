package com.ll.rest.boundarycontext.article.controller;

import com.ll.rest.base.rsData.RsData;
import com.ll.rest.boundarycontext.article.entity.Article;
import com.ll.rest.boundarycontext.article.service.ArticleService;
import com.ll.rest.boundarycontext.member.entity.Member;
import com.ll.rest.boundarycontext.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/v1/articles", produces = APPLICATION_JSON_VALUE)
@Tag(name = "ApiV1ArticlesController", description = "게시물 CRUD 컨트롤러")
public class ApiV1ArticlesController {
    private final ArticleService articleService;
    private final MemberService memberService;

    @Data
    public static class WriteRequest {
        @NotBlank
        private String subject;
        @NotBlank
        private String content;
    }

    @AllArgsConstructor
    @Getter
    public static class WriteResponse {
        private final Article article;
    }

    @PostMapping
    @Operation(summary = "등록", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<WriteResponse> write(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody WriteRequest writeRequest) {
        Member member = memberService.findByUsername(user.getUsername()).orElseThrow();
        RsData<Article> writeRs = articleService.write(member, writeRequest.getSubject(), writeRequest.getContent());

        if (writeRs.isFail()) {
            return (RsData) writeRs;
        }

        return RsData.of(
                writeRs.getResultCode(),
                writeRs.getMsg(),
                new WriteResponse(writeRs.getData())
        );
    }

    @AllArgsConstructor
    @Getter
    public static class ArticlesResponse {
        private final List<Article> articles;
    }

    @GetMapping(value = "")
    @Operation(summary = "조회")
    public RsData<ArticlesResponse> articles() {
        List<Article> articles = articleService.findAll();

        return RsData.of(
                "S-1",
                "성공",
                new ArticlesResponse(articles)
        );
    }

    @AllArgsConstructor
    @Getter
    public static class ArticleResponse {
        private final Article article;
    }

    @GetMapping(value = "/{id}")
    @Operation(summary = "단건조회")
    public RsData<ArticleResponse> article(@PathVariable Long id) {
        RsData<Article> articleRsData = RsData.produce(Article.class)
                .then(rsData -> articleService.findById(id));

        return articleRsData.mapToDto(ArticleResponse.class);
    }

    @Data
    public static class ModifyRequest {
        private String subject;
        private String content;
    }

    @AllArgsConstructor
    @Getter
    public static class ModifyResponse {
        private final Article article;
    }

    @PatchMapping(value = "/{id}", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "수정", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<ModifyResponse> modify(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ModifyRequest modifyRequest,
            @PathVariable Long id) {
        Member member = memberService.findByUsername(user.getUsername()).orElseThrow();

        RsData<Article> articleRs = RsData.produce(Article.class)
                .then(rsData -> articleService.findById(id))
                .then(rsData -> articleService.canModify(member, rsData.getData()))
                .then(rsData -> articleService.modify(rsData.getData(), modifyRequest.getSubject(), modifyRequest.getContent()));

        return articleRs.mapToDto(ModifyResponse.class);
    }
}
