package uk.lancs.sharc.model;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import uk.lancs.sharc.R;
import uk.lancs.sharc.controller.MainActivity;
import uk.lancs.sharc.service.SharcLibrary;

/**
 * <p>This class instructs how the media list view should be rendered</p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 **/
public class MediaListAdapter extends ArrayAdapter<String>
{
	private final Activity context;
	private final List<String> mediaList;
	private final int listType;//0 - media, 1 - eoi, 2 - summary info (or route or experience)
	private final ExperienceDetailsModel experienceDetails;
	public MediaListAdapter(Activity context, List<String> mediaList, int mListType)
	{
		super(context, R.layout.media_list_item, mediaList);
		this.context = context;
		this.mediaList = mediaList;
		this.listType = mListType;
		MainActivity activity = (MainActivity)context;
		experienceDetails = activity.getSelectedExperienceDetail();
	}
	public class viewHolder
	{
		public TextView txtNoComment;
		public ImageButton btnComment;
		public Button btnBack;
		public WebView webviewMedia;

	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{	View rowView = convertView;
		viewHolder holder;

		if(convertView == null){
			//inflate
			LayoutInflater inflater = context.getLayoutInflater();
			rowView= inflater.inflate(R.layout.media_list_item, null, true);
			holder = new viewHolder();
			//add items
			holder.txtNoComment = (TextView) rowView.findViewById(R.id.txtNoComment);
			holder.btnComment = (ImageButton) rowView.findViewById(R.id.btnCommentMedia);
			holder.btnBack = (Button) rowView.findViewById(R.id.btnBackToMap);
			holder.webviewMedia = (WebView) rowView.findViewById(R.id.webViewMedia);
			//set tag, for holder recycle
			rowView.setTag(holder);
		}else{
			holder = (viewHolder)rowView.getTag();
		}


		String countLine = "<h5 style='margin-left:20px;'>[Media item " + position + " of " + (mediaList.size()-1) + "]</h5>";
		//Get number of like and comment
		String htmlCode = mediaList.get(position);
		//TextView txtNoLike = (TextView) rowView.findViewById(R.id.txtNoLike);
		//ImageButton btnLike = (ImageButton) rowView.findViewById(R.id.btnLikeMedia);

		holder.webviewMedia.setTag(position);

		if(position==0 || listType == 1)
		{
			//Hide all button			
			//btnLike.setVisibility(View.GONE);
			holder.btnComment.setVisibility(View.GONE);
			//txtNoLike.setVisibility(View.GONE);
			holder.txtNoComment.setVisibility(View.GONE);
			holder.btnBack.setVisibility(View.GONE);
			countLine = "";
		}
		else
		{
			htmlCode = htmlCode.substring(htmlCode.indexOf("<span"),htmlCode.indexOf("</span>"));////000#id#1111#type#noLike#noComments#therest
			String[] params = htmlCode.split("#");
			//txtNoLike.setText(params[4]);
			holder.txtNoComment.setText(params[5]);
			//btnLike.setTag(position);
			holder.btnComment.setTag(position);
			holder.btnBack.setTag(position);
		}

		if(position == mediaList.size() - 1)
			holder.btnBack.setVisibility(View.VISIBLE);
		else
			holder.btnBack.setVisibility(View.GONE);

		if(listType != 0)
			countLine = "";
		//Content in webview

		String base = "file://" + SharcLibrary.SHARC_MEDIA_FOLDER + "/";
		if(listType == 0 && position == 0)//eoi
		{
			AndroidWebViewInterface inObj = new AndroidWebViewInterface(context);
			SharcLibrary.setupWebView(holder.webviewMedia, context, inObj);
		}
		else
			SharcLibrary.setupWebView(holder.webviewMedia, context);
		holder.webviewMedia.loadDataWithBaseURL(base, countLine + mediaList.get(position), "text/html", "utf-8", null);

		/*webviewMedia.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int position = (Integer) v.getTag();
				if(position > 0)
					Toast.makeText(getContext(), "You've selected media: " +  mediaList.get(position), Toast.LENGTH_LONG).show();
				return false;
			}
		});*/

		//Button events
		/*btnLike.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int position = (Integer) v.getTag();
				//Toast.makeText(getContext(), "You've liked " + String.valueOf(position), Toast.LENGTH_LONG).show();
				if(position > 0) {
					MainActivity activity = (MainActivity)context;
					activity.displayOneMedia(position);
					//Toast.makeText(getContext(), "You've selected media: " + mediaList.get(position), Toast.LENGTH_LONG).show();
				}
				
			}
		});*/

		holder.btnComment.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = (Integer) v.getTag();
				MainActivity activity = (MainActivity)context;
				activity.showCommentDialogForMediaItem(position);
                //Toast.makeText(getContext(), "You've commented " + String.valueOf(position), Toast.LENGTH_LONG).show();
            }
        });

		holder.btnBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MainActivity activity = (MainActivity) context;
				activity.displayMapTab();
			}
		});


		//Webview's event
		return rowView;
	}
}