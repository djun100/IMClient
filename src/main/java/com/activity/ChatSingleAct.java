package com.activity;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.tsz.afinal.annotation.view.ViewInject;
import util.Util;
import vo.Content;
import vo.Myself;
import adapter.ChatAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import application.IMApplication;
import aysntask.FetchOnlineUserTask;
import aysntask.LoginTask;
import config.Const;

public class ChatSingleAct extends BaseActivity {

    @ViewInject(id = R.id.lv_chat_detail)
    private ListView chatList;

    @ViewInject(id = R.id.send)
    private Button sendBtn;

    @ViewInject(id = R.id.content)
    private EditText input;

    private ChatAdapter adapter;

    public static int sendId = -1;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_detail);
        final Myself vo = (Myself) getVo("0");
        final Content msg = (Content) getIntent().getExtras().getSerializable("3");
        final List<Content> msgs = (List<Content>) getVo("1");
        if (Util.isEmpty(msgs)) {
            adapter = new ChatAdapter(new ArrayList<Content>(), activity);
        } else {
            sendId=msgs.get(0).getSendId();
            adapter = new ChatAdapter(msgs, activity);
        }
        if (msg != null) {
            sendId=msg.getSendId();
            adapter.addItem(msg, 0);
            HomeActivity.singleMsgs.remove(vo.getChannelId());
            sendBroadcast(ChatSingleAct.this, msg);
        }
        chatList.setAdapter(adapter);
        registerBoradcastReceiver(new msgBroadcastReceiver());
        sendBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final Content content = new Content();
                content.setDate(new Date());
                content.setMsg(input.getText().toString());
                input.setText("");
                // 指定发送消息的人为当前登录的人
                content.setSendName(LoginTask.currentName);
                content.setSendMsg(true);
                content.setSendId(db.findAll(Myself.class).get(0).getChannelId());
                if (msg != null) {
                    content.setReceiveName(msg.getSendName());
                    content.setReceiveId(msg.getSendId());
                } else {
                    if (!Util.isEmpty(msgs)) {
                        content.setReceiveId(msgs.get(0).getSendId());
                        content.setReceiveName(msgs.get(0).getSendName());
                    } else {
                        content.setReceiveName(vo.getName());
                        content.setReceiveId(vo.getChannelId());
                    }
                }
                sendId=content.getReceiveId();
                FetchOnlineUserTask.channel.writeAndFlush(content).addListener(
                        new GenericFutureListener<Future<? super Void>>() {
                            @Override
                            public void operationComplete(Future<? super Void> future)
                                    throws Exception {
                                if (future.isSuccess()) {
                                    ChatSingleAct.this.runOnUiThread(new Runnable() {

                                        @Override
                                        public void run() {
                                            adapter.addItem(content, adapter.getCount());
                                            chatList.setSelection(adapter.getCount() - 1);
                                        }
                                    });
                                }
                            }
                        });
            }
        });
    }

    class msgBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Const.ACTION_SINGLE_BROADCAST.equals(intent.getAction())) {
                Content content = (Content) intent.getSerializableExtra("msg");
                adapter.addItem(content, adapter.getCount());
                chatList.setSelection(adapter.getCount() - 1);
            }
        }

    }

    public void registerBoradcastReceiver(BroadcastReceiver receiver) {
        IntentFilter myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(Const.ACTION_SINGLE_BROADCAST);
        IMApplication.APP.registerReceiver(receiver, myIntentFilter);
    }

    /**
     * 
     * @desc:发送广播到聊天列表界面，删除掉列表上显示的条数
     * @author WY 创建时间 2014年3月14日 下午2:07:37
     * @param act
     * @param content
     */
    public void sendBroadcast(Context act, Content content) {
        Intent intent = new Intent();
        intent.setAction(Const.ACTION_DELETE_TIPS);
        intent.putExtra("content", content);
        act.sendBroadcast(intent);
    }
}