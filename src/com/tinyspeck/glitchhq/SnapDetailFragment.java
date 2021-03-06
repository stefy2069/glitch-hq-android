package com.tinyspeck.glitchhq;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.tinyspeck.android.GlitchRequest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class SnapDetailFragment extends BaseFragment {

	private glitchSnap m_currentSnap;
	private String m_secret, m_photoId, m_ownerName, m_ownerTsid;
	private View m_root;
	private boolean m_bRefreshToBottom;
	private SnapDetailFragment m_this;
	
	private SnapCommentsListAdapter m_commentsAdapter;
	private LinearListView m_commentsListView;
	
	private Button m_btnBack;
	private Button m_btnSidebar;
	private Button m_btnShare;
	private String m_backBtnName;
	
	public SnapDetailFragment()
	{
		super();
	}
	
	public SnapDetailFragment(String ownerName, String ownerTsid, String photoId, String secret, String backBtnName)
	{
		m_ownerTsid = ownerTsid;
		m_ownerName = ownerName;
		m_secret = secret;
		m_photoId = photoId;
		m_backBtnName = backBtnName;
		m_this = this;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) 
	{
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState != null) {
			restoreFromInstanceState(savedInstanceState);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
	{		
		savedInstanceState.putString("ownerName", m_ownerName);
		savedInstanceState.putString("ownerTsid", m_ownerTsid);
		savedInstanceState.putString("secret", m_secret);
		savedInstanceState.putString("photoId", m_photoId);
		savedInstanceState.putString("backBtnName", m_backBtnName);
	}
	
	private void restoreFromInstanceState(Bundle savedInstanceState)
	{
		m_ownerName = savedInstanceState.getString("ownerName");
		m_ownerTsid = savedInstanceState.getString("ownerTsid");
		m_secret = savedInstanceState.getString("secret");
		m_photoId = savedInstanceState.getString("photoId");
		m_backBtnName = savedInstanceState.getString("backBtnName");
		m_this = this;
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if (savedInstanceState != null) {
			restoreFromInstanceState(savedInstanceState);
		}
		
		View curView = ViewInit(inflater, R.layout.snap_detail_view, container);
		m_root = curView;
		
		m_btnBack = (Button) m_root.findViewById(R.id.btnBack);
		m_btnBack.setText(m_backBtnName);
		m_btnBack.setVisibility(View.VISIBLE);
		m_btnBack.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				FragmentManager fm = getFragmentManager();
				fm.popBackStack();
			}
		});
		m_btnSidebar = (Button) m_root.findViewById(R.id.btnSidebar);
		m_btnSidebar.setVisibility(View.GONE);
		
		m_btnShare = (Button) m_root.findViewById(R.id.btnShare);
		m_btnShare.setVisibility(View.VISIBLE);
		m_btnShare.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent shareIntent = new Intent(Intent.ACTION_SEND);
				shareIntent.setType("text/plain");
				shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Glitch Snap");
				String shareMessage = "Check out this snap on Glitch by " + m_ownerName + "! " + m_currentSnap.shortURL;
				shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
				startActivity(Intent.createChooser(shareIntent, "Share Glitch Snap"));
			}
		});
		
		m_commentsListView = (LinearListView)m_root.findViewById(R.id.snap_comments_listview);
		
		getSnap();
		return curView;
	}
	
	private void getSnap()
	{
		Map<String, String> params = new HashMap<String, String>();
		params.put("owner_tsid", m_ownerTsid);
		params.put("snap_id", m_photoId);
		params.put("secret", m_secret);
		
		GlitchRequest request = m_application.glitch.getRequest("snaps.detail", params);
		request.execute(this);
		
		m_requestCount = 1;
		
		if (getActivity() != null) 
			((HomeScreen)getActivity()).showSpinner(true);
	}
	
	@Override
	public void onRequestBack(String method, JSONObject response)
	{
		if (method == "snaps.detail") {
			receiveSnap(response);
			if (m_bRefreshToBottom) {
				m_bRefreshToBottom = false;
				ScrollView sv = (ScrollView) m_root.findViewById(R.id.snap_detail_scrollview);
				sv.fullScroll(ScrollView.FOCUS_DOWN);
			}
		} else if (method == "snaps.comment") {
			m_bRefreshToBottom = true;
			getSnap();
		}
		onRequestComplete();
	}
	
	private void receiveSnap(JSONObject response)
	{
		m_currentSnap = new glitchSnap();
		m_currentSnap.id = response.optInt("id");
		m_currentSnap.what = response.optString("caption");
		m_currentSnap.playerID = response.optString("who_tsid");
		m_currentSnap.who = response.optString("who_name");
		JSONObject jURL = response.optJSONObject("who_urls");
		if (jURL != null) {
			m_currentSnap.avatar = jURL.optString("singles_100");
		}
		long seconds = System.currentTimeMillis()/1000;
		int nSec = (int)seconds - response.optInt("date_create");		
		m_currentSnap.when = Util.TimeToString(nSec);
		m_currentSnap.views = response.optInt("views");		
		m_currentSnap.locationName = response.optString("location_name");
		m_currentSnap.locationHub = response.optString("location_hub");
		m_currentSnap.locationTsid = response.optString("location_tsid");
		m_currentSnap.shortURL = response.optString("short_url");
		JSONObject imageURL = response.optJSONObject("images");
		m_currentSnap.image = imageURL.optString("standard");
		m_currentSnap.image_orig = imageURL.optString("orig");
		JSONArray jsonComments = response.optJSONArray("comments");
		
		if (jsonComments != null) {
			JSONObject urls;
			int sec;
			
			m_currentSnap.comments = new Vector<glitchSnapComment>();
			for (int i=0; jsonComments != null && i < jsonComments.length(); i++) {
				glitchSnapComment c = new glitchSnapComment();
				try {
					c.playerID = jsonComments.getJSONObject(i).optString("who_tsid");
					c.who = jsonComments.getJSONObject(i).optString("who_name");
					urls = jsonComments.getJSONObject(i).optJSONObject("who_urls");
					c.avatar = urls.optString("singles_100");
					sec = (int)seconds - jsonComments.getJSONObject(i).optInt("date_create");
					c.when = Util.TimeToString(sec);
					c.what = jsonComments.getJSONObject(i).optString("text");
					m_currentSnap.comments.add(c);
				} catch (JSONException e) {
					e.printStackTrace();
				}				
			}
			
			m_commentsAdapter = new SnapCommentsListAdapter(getActivity(), m_currentSnap.comments);
			m_commentsListView.setAdapter(m_commentsAdapter);
		}
		
		setSnapDetailView(m_root);
	}
	
	protected void setSnapDetailView(View root)
	{
		OnClickListener lsn = new OnClickListener() {
			public void onClick(View v) {
				String playerId = (String)v.getTag();
				ProfileFragment f = new ProfileFragment(m_this, playerId, true);
				((HomeScreen)getActivity()).setCurrentFragment(f, true);
			}
		};
		
		TextView ownerName = (TextView) root.findViewById(R.id.snap_owner_name);
		ownerName.setTypeface(m_application.m_vagFont);
		ownerName.setText(m_currentSnap.who);
		ownerName.setTag(m_currentSnap.playerID);
		ownerName.setOnClickListener(lsn);
		
		ImageView goArrow = (ImageView) root.findViewById(R.id.snap_go_arrow);
		goArrow.setTag(m_currentSnap.playerID);
		goArrow.setOnClickListener(lsn);
		
		ImageView snapPhoto = (ImageView) root.findViewById(R.id.snap_detail_photo);
		m_application.Download(m_currentSnap.image, snapPhoto, MyApplication.DOWNLOAD_TYPE_NORMAL);
		snapPhoto.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				WebViewFragment f = new WebViewFragment(m_currentSnap.image_orig, "Snap", true);
				((HomeScreen)getActivity()).setCurrentFragment(f, true);
			}
		});
		
		TextView snapDetail = (TextView) root.findViewById(R.id.snap_detail_text);
		
		snapDetail.setText(Html.fromHtml(m_currentSnap.what));
		
		snapDetail.setVisibility((m_currentSnap.what != null && m_currentSnap.what.length() > 0)
				? View.VISIBLE : View.GONE);
		
		TextView snapTime = (TextView) root.findViewById(R.id.snap_detail_time);
		snapTime.setTypeface(m_application.m_vagLightFont);
		snapTime.setText(m_currentSnap.when);
		if (!m_currentSnap.when.equalsIgnoreCase("just now"))
			snapTime.setText(m_currentSnap.when + " ago");
		else
			snapTime.setText(m_currentSnap.when);
		
		TextView snapViews = (TextView)root.findViewById(R.id.snap_detail_views);
		snapViews.setTypeface(m_application.m_vagLightFont);
		if (m_currentSnap.views == 1)
			snapViews.setText(String.valueOf(m_currentSnap.views) + " view");
		else
			snapViews.setText(String.valueOf(m_currentSnap.views) + " views");
		
		EditText snapCommentEditor = (EditText) root.findViewById(R.id.snap_comment_editor);
		snapCommentEditor.setHint("Comment on " + m_currentSnap.who + "'s snap...");
		snapCommentEditor.setImeActionLabel("Done", EditorInfo.IME_ACTION_DONE);
		snapCommentEditor.setImeOptions(EditorInfo.IME_ACTION_DONE);
		snapCommentEditor.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				
				if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE) {					
					postSnapComment(v.getText().toString());
					v.setText("");
					
					InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					mgr.hideSoftInputFromWindow( v.getWindowToken(), 0);
				}
				return false;
			}
		});
		
		LinearLayout snapCommentsView = (LinearLayout) root.findViewById(R.id.snap_comments_view);
		if (m_currentSnap.comments != null && m_currentSnap.comments.size() > 0)
			snapCommentsView.setVisibility(View.VISIBLE);
		else
			snapCommentsView.setVisibility(View.GONE);
	}
	
	private void postSnapComment(String comment) 
	{
		Map<String, String> params = new HashMap<String, String>();
		params.put("owner_tsid", m_ownerTsid);
		params.put("snap_id", m_photoId);
		params.put("comment", comment);
		
		GlitchRequest request = m_application.glitch.getRequest("snaps.comment", params);
		request.execute(this);
		
		m_requestCount = 1;
		if (getActivity() != null)
			((HomeScreen)getActivity()).showSpinner(true);
	}
	
	protected boolean doesSupportRefresh()
	{
		return true;
	}
	
	protected void onRefresh()
	{
		getSnap();
	}
}