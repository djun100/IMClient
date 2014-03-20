package com.activity;

import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.tsz.afinal.FinalDb;
import util.FileOperator;
import util.Util;
import vo.ChatRoom;
import vo.Content;
import vo.Myself;
import vo.OnlineFriends;
import vo.RoomChild;
import adapter.ChatAdapter;
import adapter.GroupChatAdapter;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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

public class ChatGroupAct extends BaseActivity {

    private Button addFriend;

    private ListView chatList;

    private Button sendBtn;

    private EditText input;

    ChannelFuture lastWriteFuture = null;

    ChatAdapter chatAdapter;

    private List<RoomChild> friends;

    public static Long CurrentGroup = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        chatAdapter = new ChatAdapter(new ArrayList<Content>(), activity);
        if (!Util.isEmpty(HomeActivity.groupMsgs.get(((ChatRoom) getVo("0")).getGrouppTag()))) {
            chatAdapter
                    .addItems(HomeActivity.groupMsgs.get(((ChatRoom) getVo("0")).getGrouppTag()));
            HomeActivity.groupMsgs.get((((ChatRoom) getVo("0")).getGrouppTag())).clear();
        }
        setContentView(R.layout.group_chat);
        initView();
        registerBoradcastReceiver(new msgBroadcastReceiver());
        friends = ((ChatRoom) getVo("0")).getChildDatas();
        CurrentGroup = friends.get(0).getGroupTag();

    }

    public void initView() {
        addFriend = (Button) findViewById(R.id.add_friend);
        chatList = (ListView) findViewById(R.id.lv_chat_detail);
        sendBtn = (Button) findViewById(R.id.send);
        input = (EditText) findViewById(R.id.content);
        chatList.setAdapter(chatAdapter);
        addFriend.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("请选择好友");
                builder.setIcon(android.R.drawable.ic_dialog_info);
                View view = makeView(R.layout.group_chat_list);
                builder.setView(view);
                ListView list = (ListView) view.findViewById(R.id.group_chat_list);
                FinalDb db = FinalDb.create(activity, FileOperator.getDbPath(activity), true);
                //TODO获取在线的好友列表
                
                List<OnlineFriends> onlines = db.findAll(OnlineFriends.class);
                List<Myself> onlineUser = new ArrayList<Myself>();
                if (!Util.isEmpty(onlines)) {
                    for (OnlineFriends on : onlines) {
                        Myself me = new Myself();
                        me.setChannelId(on.getChannelId());
                        me.setName(on.getName());
                        onlineUser.add(me);
                    }
                }
                
                List<RoomChild> src = new ArrayList<RoomChild>();
                if (!Util.isEmpty(onlineUser)) {
                    for (Myself u : onlineUser) {
                        RoomChild child = new RoomChild();
                        child.setChannelId(u.getChannelId());
                        child.setName(u.getName());
                        src.add(child);
                    }
                }
                List<RoomChild> existChilds = friends;
                if (!Util.isEmpty(onlineUser)) {
                    for (int i = 0; i < onlineUser.size(); i++) {
                        for (RoomChild user : existChilds) {
                            if (onlineUser.get(i).getChannelId() == user.getChannelId()) {
                                onlineUser.remove(i);
                                i--;
                                break;
                            }
                        }
                    }
                }
                final GroupChatAdapter gcAdapter = new GroupChatAdapter(src, activity);
                list.setAdapter(gcAdapter);
                builder.setPositiveButton("确定", new Dialog.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        List<RoomChild> tempList = new ArrayList<RoomChild>();
                        for (int i = 0; i < gcAdapter.isChecked.size(); i++) {
                            if (gcAdapter.isChecked.get(i)) {
                                RoomChild checkedUser = gcAdapter.getItem(i);
                                RoomChild child = new RoomChild();
                                child.setChannelId(checkedUser.getChannelId());
                                child.setName(checkedUser.getName());
                                tempList.add(child);
                            }
                        }
                        if (!Util.isEmpty(tempList)) {
                            friends.addAll(tempList);
                        }
                    }
                });
                builder.create();
                builder.show();
            }
        });

        sendBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final Content content = new Content();
                content.setDate(new Date());
                content.setMsg(input.getText().toString());
                input.setText("");
                // 指定发送者为当前登录的人
                content.setSendName(LoginTask.currentName);
                content.setSendMsg(true);
                content.setReceiveId(0);
                content.setGrouppTag(CurrentGroup);
                List<Integer> ids = new ArrayList<Integer>();
                FinalDb db = FinalDb.create(activity, FileOperator.getDbPath(activity), true);
                for (RoomChild user : friends) {
                    if (user.getChannelId() != db.findAll(Myself.class).get(0).getChannelId()) {
                        ids.add(user.getChannelId());
                    }
                }
                content.setTargetIds(ids);
                lastWriteFuture = FetchOnlineUserTask.channel.writeAndFlush(content);
                lastWriteFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        if (future.isSuccess()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    chatAdapter.addItem(content, chatAdapter.getCount());
                                    chatList.setSelection(chatAdapter.getCount() - 1);
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
            if (Const.ACTION_GROUP_CHAT.equals(intent.getAction())) {
                Content content = (Content) intent.getSerializableExtra("msg");
                chatAdapter.addItem(content, chatAdapter.getCount());
                chatList.setSelection(chatAdapter.getCount() - 1);
            }
        }

    }

    public void registerBoradcastReceiver(BroadcastReceiver receiver) {
        IntentFilter myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(Const.ACTION_GROUP_CHAT);
        // 注册广播
//        registerReceiver(receiver, myIntentFilter);
        IMApplication.APP.reReceiver(receiver, myIntentFilter);
    }
}