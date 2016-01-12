package uk.lancs.sharc.model;

import java.util.ArrayList;
import java.util.List;

import uk.lancs.sharc.R;
import uk.lancs.sharc.controller.MainActivity;
import uk.lancs.sharc.service.SharcLibrary;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings.PluginState;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
		
	@Override
	public View getView(int position, View view, ViewGroup parent) 
	{
		LayoutInflater inflater = context.getLayoutInflater();
		View rowView= inflater.inflate(R.layout.media_list_item, null, true);

		String countLine = "<h5 style='margin-left:20px;'>[Media item " + position + " of " + (mediaList.size()-1) + "]</h5>";
		//Get number of like and comment
		String htmlCode = mediaList.get(position);
		TextView txtNoLike = (TextView) rowView.findViewById(R.id.txtNoLike);
		ImageButton btnLike = (ImageButton) rowView.findViewById(R.id.btnLikeMedia);
		TextView txtNoComment = (TextView) rowView.findViewById(R.id.txtNoComment);
		ImageButton btnComment = (ImageButton) rowView.findViewById(R.id.btnCommentMedia);
		Button btnBack = (Button) rowView.findViewById(R.id.btnBackToMap);
		WebView webviewMedia = (WebView) rowView.findViewById(R.id.webViewMedia);
		webviewMedia.setTag(position);

		if(position==0 || listType == 1)
		{
			//Hide all button			
			btnLike.setVisibility(View.GONE);
			btnComment.setVisibility(View.GONE);
			txtNoLike.setVisibility(View.GONE);
			txtNoComment.setVisibility(View.GONE);
			btnBack.setVisibility(View.GONE);
			countLine = "";
		}
		else
		{
			htmlCode = htmlCode.substring(htmlCode.indexOf("<span"),htmlCode.indexOf("</span>"));////000#id#1111#type#noLike#noComments#therest
			String[] params = htmlCode.split("#");
			txtNoLike.setText(params[4]);
			txtNoComment.setText(params[5]);
			btnLike.setTag(position);
			btnComment.setTag(position);
			btnBack.setTag(position);
		}

		if(position == mediaList.size() - 1)
			btnBack.setVisibility(View.VISIBLE);
		else
			btnBack.setVisibility(View.GONE);

		if(listType != 0)
			countLine = "";
		//Content in webview

		String base = "file://" + SharcLibrary.SHARC_MEDIA_FOLDER + "/";
		if(listType == 0 && position == 0)//eoi
		{
			AndroidWebViewInterface inObj = new AndroidWebViewInterface(context);
			SharcLibrary.setupWebView(webviewMedia, context, inObj);
		}
		else
			SharcLibrary.setupWebView(webviewMedia, context);
		webviewMedia.loadDataWithBaseURL(base, countLine + mediaList.get(position), "text/html", "utf-8", null);

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
		btnLike.setOnClickListener(new OnClickListener() {
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
		});

        btnComment.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = (Integer) v.getTag();
				MainActivity activity = (MainActivity)context;
				activity.showCommentDialogForMediaItem(position);
                //Toast.makeText(getContext(), "You've commented " + String.valueOf(position), Toast.LENGTH_LONG).show();
            }
        });

		btnBack.setOnClickListener(new OnClickListener() {
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