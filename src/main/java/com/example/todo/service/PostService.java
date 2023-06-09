package com.example.todo.service;

import com.example.todo.config.security.JwtTokenProvider;
import com.example.todo.dto.PostDTO;
import com.example.todo.entities.LikeEntity;
import com.example.todo.entities.PostEntity;
import com.example.todo.entities.UserEntity;
import com.example.todo.repository.FollowRepository;
import com.example.todo.repository.LikeRepository;
import com.example.todo.repository.PostRepository;
import com.example.todo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostService {
    // todo 유효성 추가

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private FollowRepository followRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public List<PostEntity> findAllByUserId(HttpServletRequest request, Pageable pageable) throws Exception {
        String email = jwtTokenProvider.getCurrentUser(request);
        return postRepository.findAllByEmail(email, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "postIdx")));
    }

    @Transactional
    public PostDTO save(PostDTO post, HttpServletRequest request) throws Exception {
        String email = jwtTokenProvider.getCurrentUser(request);
        Optional<UserEntity> userEntity = userRepository.findByEmail(email);
        if(userEntity.isEmpty()) {
            throw new Exception("존재하지 않는 이메일입니다.");
        } else {
            UserEntity user = userEntity.get();
            PostEntity postEntity = post.toEntity();
            PostEntity newPost = PostEntity.builder()
                    .user(user)
                    .description(postEntity.getDescription())
                    .shared(false)
                    .endDate(postEntity.getEndDate())
                    .likeCnt(0L)
                    .startDate(LocalDateTime.now())
                    .title(postEntity.getTitle())
                    .completed(false)
                    .build();
            postRepository.saveAndFlush(newPost);
            return newPost.toDTO();
        }
    }

    public PostDTO update(PostDTO post) throws Exception {
        // 수정 항목 : description, shared, likeCnt, title, completed
        PostEntity postEntity = post.toEntity();
        Optional<PostEntity> postOption = postRepository.findById(postEntity.getPostIdx());
        if(postOption.isEmpty()) {
            throw new Exception("존재하지 않는 post id입니다.");
        } else {
            PostEntity findPost = postOption.get();
            findPost.setDescription(postEntity.getDescription());
            findPost.setShared(postEntity.isShared());
            findPost.setLikeCnt(postEntity.getLikeCnt());
            findPost.setCompleted(postEntity.isCompleted());
            postRepository.save(findPost);
            return findPost.toDTO();
        }
    }

    @Transactional
    public void delete(Long postId) {
        postRepository.deleteById(postId);
        postRepository.flush();
    }

    public List<PostEntity> findSharedPosts() {
        return postRepository.findSharedPosts();
    }

    public int getLikeCnt(Long postId) {
        return postRepository.getLikeCnt(postId);
    }

    public PostEntity findOneByPostIdx(Long postId) {
        return postRepository.findAllByPostIdx(postId);
    }

    @Transactional
    public ResponseEntity<String> addLike(PostDTO post, HttpServletRequest request) throws Exception {
        PostEntity writerEntity = postRepository.findById(post.getPostIdx()).get();
        if(writerEntity.getUser() == null) {
            throw new Exception("작성자를 찾을 수 없습니다.");
        }

        String email = jwtTokenProvider.getCurrentUser(request);
        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("사용자를 찾을 수 없습니다"));
        PostEntity postEntity = postRepository.findById(post.getPostIdx()).get();

        if(writerEntity.getUser().getUserIdx().equals(userEntity.getUserIdx())) {
            throw new Exception("같은 유저는 좋아요를 할 수 없습니다.");
        }

        // 친구 관계인지 확인
        if(!followRepository.findAllByToUser(userEntity.getUserIdx()).contains(writerEntity.getUser())
                && !followRepository.findAllByFromUser(userEntity.getUserIdx()).contains(writerEntity.getUser())) {
            throw new Exception("친구 관계가 아니므로 좋아요를 할 수 없습니다.");
        }

        Optional<LikeEntity> findLikeEntity = likeRepository.findByPostAndUser(postEntity, userEntity);
        if(findLikeEntity.isPresent()) {
            likeRepository.delete(findLikeEntity.get());
            if(postEntity.getLikeCnt() > 0) {
                postEntity.setLikeCnt(postEntity.getLikeCnt()-1);
                return ResponseEntity.ok("좋아요 취소 성공");
            } else {
                throw new Exception("좋아요를 취소할 수 없습니다.");
            }
        } else {
            likeRepository.saveAndFlush(new LikeEntity(postEntity, userEntity));
            postEntity.setLikeCnt(postEntity.getLikeCnt()+1);
            return ResponseEntity.ok("좋아요 성공");
        }
    }
    public List<PostEntity> searchPost(String keyword, HttpServletRequest request) throws Exception {
        String email = jwtTokenProvider.getCurrentUser(request);
        List<PostEntity> posts = postRepository.findAllByEmailAndKeyword(email, keyword);

        if(posts.isEmpty())
            throw new Exception(keyword+"와(과) 일치하는 검색 결과가 없습니다.");

        return posts;
    }
}
