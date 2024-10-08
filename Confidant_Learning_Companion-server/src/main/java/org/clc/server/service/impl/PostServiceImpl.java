package org.clc.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.clc.common.constant.MessageConstant;
import org.clc.common.context.BaseContext;
import org.clc.pojo.dto.PageQueryDto;
import org.clc.pojo.dto.PostDto;
import org.clc.pojo.dto.PostIdDto;
import org.clc.pojo.entity.*;
import org.clc.pojo.entity.enumeration.OperationType;
import org.clc.server.mapper.*;
import org.clc.common.result.PageResult;
import org.clc.common.result.Result;
import org.clc.server.service.PostService;
import org.clc.common.utils.MyRandomStringGenerator;
import org.clc.common.utils.OperationLogsUtil;
import org.clc.pojo.vo.PostDetailVo;
import org.clc.pojo.vo.PostVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @version 1.0
 * @description: TODO
 */
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private LearnerMapper learnerMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private TagPostMapper tagPostMapper;

    @Autowired
    private LearnerFavorPostMapper learnerFavorPostMapper;

    @Autowired
    private OperationLogsMapper operationLogsMapper;

    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;

    @Override
    public PageResult getFavorPost(PageQueryDto pageQueryDto) {
        String uid= BaseContext.getCurrentId();
        QueryWrapper<LearnerFavorPost> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", uid);
        List<LearnerFavorPost> learnerFavorPosts = learnerFavorPostMapper.selectList(queryWrapper);
        List<String> postIdList = new ArrayList<>();
        if(learnerFavorPosts!=null){
            for (LearnerFavorPost learnerFavorPost:learnerFavorPosts){
                postIdList.add(learnerFavorPost.getPostId());
            }
        }else{
            return new PageResult(0,0,Collections.EMPTY_LIST);
        }
        if(!postIdList.isEmpty()){
            Page<Post> page=Page.of(pageQueryDto.getPage(),pageQueryDto.getPageSize());
            LambdaQueryWrapper<Post> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper.in("post_id", postIdList);
            Page<Post> p = postMapper.selectPage(page, queryWrapper1);
            //返回帖子的作者信息，标签信息
            List<Post> posts = p.getRecords();
            List<PostVo> postVos = getPostsVo(posts);
            return new PageResult(p.getTotal(),p.getPages(),postVos);
        }else{
            return new PageResult(0,0,Collections.EMPTY_LIST);
        }
    }

    @Override
    public PageResult getPosts(PageQueryDto pageQueryDto) {
        Page<Post> page=Page.of(pageQueryDto.getPage(),pageQueryDto.getPageSize());
        Page<Post> p=postMapper.selectPage(page,new QueryWrapper<>());
        //返回帖子的作者信息，标签信息
        List<Post> posts = p.getRecords();
        List<PostVo> postVos = getPostsVo(posts);
        return new PageResult(p.getTotal(),p.getPages(),postVos);
    }

    @Override
    public Result<String> ban(PostIdDto postIdDto) {
        String postId=postIdDto.getPostId();
        Post post=selectByPostId(postId);
        post.setStatus(false);
        // 删除 Redis 中的缓存数据
        redisTemplate.delete(postId);
        try {
            postMapper.updateById(post);
            //建立操作日志
            OperationLogs operationLogs= OperationLogsUtil.buildOperationLog(
                    OperationType.BAN_POST,
                    postIdDto.getPostId(),
                    true,
                    MessageConstant.SUCCESS
            );
            operationLogsMapper.insert(operationLogs);//添加操作日志
            return Result.success();
        } catch (Exception e) {
            //建立操作日志
            OperationLogs operationLogs= OperationLogsUtil.buildOperationLog(
                    OperationType.BAN_POST,
                    postIdDto.getPostId(),
                    false,
                    e.getMessage()
            );
            operationLogsMapper.insert(operationLogs);//添加操作日志
            return Result.error(500,MessageConstant.FAILED);
        }
    }

    @Override
    public Result<String> unban(PostIdDto postIdDto) {
        String postId=postIdDto.getPostId();
        Post post=selectByPostId(postId);
        post.setStatus(true);
        try {
            postMapper.updateById(post);
            //建立操作日志
            OperationLogs operationLogs= OperationLogsUtil.buildOperationLog(
                    OperationType.UNBAN_USER,
                    postIdDto.getPostId(),
                    true,
                    MessageConstant.SUCCESS
            );
            operationLogsMapper.insert(operationLogs);//添加操作日志
            return Result.success();
        } catch (Exception e) {
            //建立操作日志
            OperationLogs operationLogs= OperationLogsUtil.buildOperationLog(
                    OperationType.UNBAN_USER,
                    postIdDto.getPostId(),
                    false,
                    e.getMessage()
            );
            operationLogsMapper.insert(operationLogs);//添加操作日志
            return Result.error(500,MessageConstant.FAILED);
        }
    }

    @Override
    public PostDetailVo getPostDetail(Post post) {
        PostDetailVo postDetailVo=new PostDetailVo();
        BeanUtils.copyProperties(post,postDetailVo);
        postDetailVo.setUsername(learnerMapper.selectOne(new QueryWrapper<Learner>().eq("uid",post.getUid())).getUsername());
        postDetailVo.setLearnerImage(learnerMapper.selectOne(new QueryWrapper<Learner>().eq("uid",post.getUid())).getImage());
        List<TagPost> tagPosts=tagPostMapper.selectList(new QueryWrapper<TagPost>().eq("post_id",post.getPostId()));
        List<Tag> tags=new ArrayList<>();
        for(TagPost tagPost:tagPosts){
            tags.add(tagMapper.selectById(tagPost.getTagId()));
        }
        postDetailVo.setTags(tags);
        return postDetailVo;
    }


    @Override
    public List<PostVo> getPostsVo(List<Post> posts) {
        List<PostVo> postVos = new ArrayList<>();
        for(Post post:posts){
            PostVo postVo=new PostVo();
            BeanUtils.copyProperties(post,postVo);
            postVo.setUsername(learnerMapper.selectOne(new QueryWrapper<Learner>().eq("uid",post.getUid())).getUsername());
            postVo.setLearnerImage(learnerMapper.selectOne(new QueryWrapper<Learner>().eq("uid",post.getUid())).getImage());
            List<TagPost> tagPosts=tagPostMapper.selectList(new QueryWrapper<TagPost>().eq("post_id",post.getPostId()));
            List<Tag> tags=new ArrayList<>();
            for(TagPost tagPost:tagPosts){
                tags.add(tagMapper.selectById(tagPost.getTagId()));
            }
            postVo.setTags(tags);
            postVos.add(postVo);
        }
        return postVos;
    }

    @Override
    public Result<String> addPost(PostDto postDto) {
        Post post=new Post();
        BeanUtils.copyProperties(postDto,post);
        post.setUid(BaseContext.getCurrentId());
        post.setPostId(MessageConstant.PREFIX_FOR_POST+MyRandomStringGenerator.generateRandomString(8));
        post.setThumbs(0);
        post.setStatus(true);
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        try{
            postMapper.insert(post);
            return Result.success(MessageConstant.SUCCESS);
        }catch (RuntimeException e){
            return Result.error(500,MessageConstant.FAILED);
        }
    }

    @Override
    public void thumbComment(String postId) {
        // 使用postId作为key，从Redis中获取点赞数
        Integer likes = redisTemplate.opsForValue().get(postId);
        if (likes == null) {
            // 如果Redis中没有记录，需要从数据库加载初始值
            likes = selectByPostId(postId).getThumbs();
        }
        // 点赞数加一
        likes++;
        // 将更新后的点赞数存回Redis
        redisTemplate.opsForValue().set(postId, likes);
        // 异步更新数据库
        updateLikesInDatabaseAsync(postId, likes);
    }

    @Async
    protected void updateLikesInDatabaseAsync(String postId, Integer likes) {
        Post post=selectByPostId(postId);
        post.setThumbs(likes);
        postMapper.updateById(post);
    }

    private Post selectByPostId(String postId) {
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("post_id", postId);
        return postMapper.selectOne(queryWrapper);
    }
}
