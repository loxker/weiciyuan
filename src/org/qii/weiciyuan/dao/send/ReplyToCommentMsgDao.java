package org.qii.weiciyuan.dao.send;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.qii.weiciyuan.bean.CommentBean;
import org.qii.weiciyuan.dao.URLHelper;
import org.qii.weiciyuan.support.error.WeiboException;
import org.qii.weiciyuan.support.http.HttpMethod;
import org.qii.weiciyuan.support.http.HttpUtility;
import org.qii.weiciyuan.support.utils.ActivityUtils;
import org.qii.weiciyuan.support.utils.AppLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * User: qii
 * Date: 12-8-28
 */
public class ReplyToCommentMsgDao {

    public CommentBean reply() {
        String url = URLHelper.reply_Comment();
        Map<String, String> map = new HashMap<String, String>();
        map.put("access_token", access_token);
        map.put("id", id);
        map.put("cid", cid);
        map.put("comment", comment);
        map.put("comment_ori", comment_ori);
        map.put("without_mention", without_mention);

        String jsonData = null;
        try {
            jsonData = HttpUtility.getInstance().executeNormalTask(HttpMethod.Post, url, map);
        } catch (WeiboException e) {
            e.printStackTrace();

        }

        Gson gson = new Gson();

        CommentBean value = null;
        try {
            value = gson.fromJson(jsonData, CommentBean.class);
        } catch (JsonSyntaxException e) {
            ActivityUtils.showTips("发生错误，请重刷");
            AppLogger.e(e.getMessage().toString());
        }


        return value;

    }

    public ReplyToCommentMsgDao(String token, CommentBean bean, String replyContent) {
        this.access_token = token;
        this.cid = bean.getId();
        this.id = bean.getStatus().getId();
        this.comment = replyContent;
    }

    private String access_token;
    private String cid;
    private String id;
    private String comment;
    private String without_mention;
    private String comment_ori;
}