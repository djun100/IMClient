package adapter;

import java.io.IOException;
import java.util.List;

import org.springframework.util.support.Base64;

import vo.Content;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.activity.R;

public class ChatAdapter extends SimpleAdapter<Content> {

	private int COME_MSG = 0;

	private int SEND_MSG = 1;

	public ChatAdapter(List<Content> data, Context activity) {
		super(data, activity);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Content item = getItem(position);
		Holder holder = null;
		if (convertView == null) {
			holder = new Holder();
			if (item.isSendMsg()) {
				convertView = makeView(R.layout.item_chat_send);
			} else {
				convertView = makeView(R.layout.item_chat_receive);
			}
			holder.msg = (TextView) convertView.findViewById(R.id.sender_msg);
			holder.name = (TextView) convertView.findViewById(R.id.sender_name);
			holder.image = (ImageView) convertView
					.findViewById(R.id.sender_image);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}
		if (item != null) {
			if (item.getImageSrc()!=null&&item.getImageSrc().length() > 0) {
				try {
					holder.image.setVisibility(View.VISIBLE);
					holder.msg.setVisibility(View.GONE);
					byte[] bitmapArray = Base64.decode(item.getImageSrc());
					Bitmap bit = BitmapFactory.decodeByteArray(bitmapArray, 0,
							bitmapArray.length);
					holder.image.setImageBitmap(bit);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				holder.image.setVisibility(View.GONE);
				holder.msg.setVisibility(View.VISIBLE);
				holder.msg.setText(item.getMsg());
			}
			holder.name.setText(item.getSendName());

		}
		return convertView;
	}

	@Override
	public int getItemViewType(int position) {
		Content item = getItem(position);
		if (item.isSendMsg()) {
			return SEND_MSG;
		} else {
			return COME_MSG;
		}
	}

	@Override
	public int getViewTypeCount() {
		// 这个方法默认返回1，如果希望listview的item都是一样的就返回1，我们这里有两种风格，返回2
		return 2;
	}

	private static class Holder {

		private TextView name;

		private TextView msg;

		private ImageView image;
	}
}
