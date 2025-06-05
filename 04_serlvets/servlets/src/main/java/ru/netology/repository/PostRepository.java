package ru.netology.repository;

import ru.netology.model.Post;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// Stub
public class PostRepository {
  private final ConcurrentHashMap<Long, Post> posts = new ConcurrentHashMap<>();
  private final AtomicLong counter = new AtomicLong(1);

  public List<Post> all() {
    return new ArrayList<>(posts.values());
  }

  public Optional<Post> getById(long id) {
    return Optional.ofNullable(posts.get(id));
  }

  public Post save(Post post) {
    if (post.getId() == 0) {
      // Create new post
      long id = counter.getAndIncrement();
      post.setId(id);
      posts.put(id, post);
      return post;
    } else {
      // Update existing post
      return posts.computeIfPresent(post.getId(), (key, existingPost) -> {
        existingPost.setContent(post.getContent());
        return existingPost;
      });
    }
  }

  public void removeById(long id) {
    posts.remove(id);
  }
}
