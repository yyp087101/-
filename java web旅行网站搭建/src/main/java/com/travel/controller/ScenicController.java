package com.travel.controller;

import com.github.pagehelper.PageInfo;
import com.travel.entity.Comment;
import com.travel.entity.Scenic;
import com.travel.entity.User;
import com.travel.service.AmapRouteService;
import com.travel.service.CommentService;
import com.travel.service.FavoriteService;
import com.travel.service.ScenicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/scenic")
public class ScenicController {

    private static final DateTimeFormatter COMMENT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private ScenicService scenicService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private AmapRouteService amapRouteService;

    @GetMapping("/list")
    public String list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "9") int pageSize,
                       Model model) {
        PageInfo<Scenic> pageInfo = scenicService.findAll(pageNum, pageSize);
        model.addAttribute("pageInfo", pageInfo);
        model.addAttribute("cities", scenicService.getAllCities());
        return "scenic/list";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String keyword,
                         @RequestParam(required = false) String city,
                         @RequestParam(required = false) String scenicType,
                         @RequestParam(defaultValue = "1") int pageNum,
                         @RequestParam(defaultValue = "9") int pageSize,
                         Model model) {
        PageInfo<Scenic> pageInfo = scenicService.search(keyword, city, scenicType, pageNum, pageSize);
        model.addAttribute("pageInfo", pageInfo);
        model.addAttribute("keyword", keyword);
        model.addAttribute("city", city);
        model.addAttribute("scenicType", scenicType);
        model.addAttribute("cities", scenicService.getAllCities());
        return "scenic/search";
    }

    @GetMapping("/filter")
    public String filter(@RequestParam(required = false) String city,
                         @RequestParam(required = false) String scenicLevel,
                         @RequestParam(required = false) String scenicType,
                         @RequestParam(required = false) String bestSeason,
                         @RequestParam(required = false) Boolean isFree,
                         @RequestParam(defaultValue = "1") int pageNum,
                         @RequestParam(defaultValue = "9") int pageSize,
                         Model model) {
        PageInfo<Scenic> pageInfo = scenicService.filter(city, scenicLevel, scenicType, bestSeason, isFree, pageNum, pageSize);
        model.addAttribute("pageInfo", pageInfo);
        model.addAttribute("city", city);
        model.addAttribute("scenicLevel", scenicLevel);
        model.addAttribute("scenicType", scenicType);
        model.addAttribute("bestSeason", bestSeason);
        model.addAttribute("isFree", isFree);
        model.addAttribute("cities", scenicService.getAllCities());
        return "scenic/filter";
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Integer id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        Integer userId = user != null ? user.getId() : null;

        Scenic scenic = scenicService.getDetail(id, userId);
        if (scenic == null) {
            return "redirect:/scenic/list";
        }
        amapRouteService.enrichScenicCoordinate(scenic);

        List<Comment> comments = commentService.findByScenicId(id);
        Double avgRating = commentService.avgRatingByScenicId(id);
        List<Scenic> similarScenics = scenicService.getRecommendations(userId);

        model.addAttribute("scenic", scenic);
        model.addAttribute("comments", comments);
        model.addAttribute("avgRating", avgRating != null ? String.format("%.1f", avgRating) : "暂无");
        model.addAttribute("commentCount", comments.size());
        model.addAttribute("similarScenics", similarScenics);

        return "scenic/detail";
    }

    @GetMapping("/hot")
    public String hotScenics(Model model) {
        List<Scenic> hotScenics = scenicService.getTopByRating(10);
        model.addAttribute("hotScenics", hotScenics);
        return "scenic/hot";
    }

    @PostMapping("/comment")
    @ResponseBody
    public Map<String, Object> addComment(@RequestParam Integer scenicId,
                                          @RequestParam String content,
                                          @RequestParam Integer rating,
                                          HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return buildResponse(401, "请先登录");
        }

        Comment comment = new Comment();
        comment.setUserId(user.getId());
        comment.setScenicId(scenicId);
        comment.setContent(content);
        comment.setRating(rating);
        comment.setCreateTime(new Date());
        comment.setUser(user);

        if (!commentService.add(comment)) {
            return buildResponse(500, "评论失败");
        }

        Map<String, Object> data = buildResponse(200, "评论成功");
        Map<String, Object> commentData = new LinkedHashMap<>();
        commentData.put("nickname", user.getNickname());
        commentData.put("content", comment.getContent());
        commentData.put("rating", comment.getRating());
        commentData.put("createTime", COMMENT_TIME_FORMAT.format(
                comment.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
        data.put("comment", commentData);

        Double avgRating = commentService.avgRatingByScenicId(scenicId);
        if (avgRating != null) {
            data.put("avgRating", String.format("%.1f", avgRating));
        }
        return data;
    }

    @PostMapping("/favorite/toggle")
    @ResponseBody
    public Map<String, Object> toggleFavorite(@RequestParam Integer scenicId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return buildResponse(401, "请先登录");
        }

        boolean favorited = favoriteService.toggle(user.getId(), scenicId);
        Map<String, Object> data = buildResponse(200, favorited ? "收藏成功" : "已取消收藏");
        data.put("favorited", favorited);
        return data;
    }

    @GetMapping("/favorites")
    public String favorites(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/user/login";
        }
        model.addAttribute("favorites", favoriteService.getUserFavorites(user.getId()));
        return "scenic/favorites";
    }

    @GetMapping("/route/plan")
    @ResponseBody
    public Map<String, Object> planRoute(@RequestParam Integer scenicId,
                                         @RequestParam(required = false) String origin,
                                         @RequestParam(required = false) Double originLat,
                                         @RequestParam(required = false) Double originLon) {
        Scenic scenic = scenicService.findById(scenicId);
        if (scenic == null) {
            return buildResponse(404, "景点不存在");
        }

        String originInput = origin;
        if (originLat != null && originLon != null) {
            originInput = originLat + "," + originLon;
        }
        return amapRouteService.planRoute(scenic, originInput);
    }

    private Map<String, Object> buildResponse(int code, String msg) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("msg", msg);
        return data;
    }
}
