package com.closeby.clzby.fragment;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;

import com.closeby.clzby.AppData;
import com.closeby.clzby.MyLocation;
import com.closeby.clzby.R;
import com.closeby.clzby.activity.Global;
import com.closeby.clzby.activity.HomeActivity;
import com.closeby.clzby.customcontrol.CircleImageView;
import com.closeby.clzby.customcontrol.CustomFontTextView;
import com.closeby.clzby.customcontrol.DialogHelper;
import com.closeby.clzby.listener.OnClickMenuItemListener;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.squareup.picasso.Picasso;
import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.ParameterMap;
import com.turbomanage.httpclient.android.AndroidHttpClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by iGold on 6/3/15.
 */


public class BusinessFragment extends BaseFragment {

    ListView mListView;
    private MyCustomAdapter mAdapter;

    JSONArray arrayData = new JSONArray(), arrayDataSource = new JSONArray();


    public static BusinessFragment newInstance() {
        BusinessFragment fragment = new BusinessFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_business, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView(view);
    }

    private void initView(View view) {

        mListView = (ListView) view.findViewById(R.id.listview);
        mAdapter = new MyCustomAdapter();
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                try {
                    JSONObject obj = arrayDataSource.getJSONObject(i);

                    ((HomeActivity) getActivity()).gotoProfile(obj.getString("UserID"));

                } catch (Exception e) {e.printStackTrace();}
            }
        });



        SearchView searchView = (SearchView) view.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                if (arrayData == null) {
                    return false;
                }

                arrayDataSource = MyJSON.clearJSONArray(arrayDataSource);


                if (newText.length() < 1) {
                    arrayDataSource = arrayData;
                }
                else {
                    for (int i = 0 ; i < arrayData.length() ; i ++) {

                        try {
                            JSONObject obj = arrayData.getJSONObject(i);


                            String businessName = obj.getString("BusinessName");
                            String categories = obj.getString("CategoryName");

                            if ((businessName != null && businessName.toUpperCase().contains(newText.toUpperCase()))
                                    || (categories != null && categories.toUpperCase().contains(newText.toUpperCase()))) {

                                arrayDataSource = MyJSON.addJSONObject(arrayDataSource, obj);
                            }
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                mAdapter.setItem(arrayDataSource);
                mAdapter.notifyDataSetChanged();

                return false;
            }
        });


        // network

        final Dialog waitDialog = DialogHelper.getWaitDialog(getActivity());
        waitDialog.show();

        AndroidHttpClient httpClient = new AndroidHttpClient(Global.kServerURL);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("guid", Global.kGUID)
                .add("UserID", AppData.getInstance().loadLoginUserID())
                .add("userlat", "" + MyLocation.getInstance().getLatitude())
                .add("userlong", "" + MyLocation.getInstance().getLongitude())
                .add("range", "" + AppData.RANGE);


        httpClient.get("/GetBusinessesWithinRange.aspx", params, new AsyncCallback() {

            @Override
            public void onComplete(HttpResponse httpResponse) {

                waitDialog.dismiss();

                try {
                    JSONObject result = new JSONObject(httpResponse.getBodyAsString());

                    if (result.getString("Success").equals("Success")) {

                        arrayData = result.getJSONObject("Data").getJSONArray("BusinessListing");
                        arrayDataSource = arrayData;

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // TODO Auto-generated method stub

                                mAdapter.setItem(arrayDataSource);
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                    } else {

                        DialogHelper.showToast(getActivity(), result.getString("Message"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }


    public class MyCustomAdapter extends BaseAdapter {


        private JSONArray items = new JSONArray();
        private LayoutInflater mInflater;


        public MyCustomAdapter() {
            mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setItem(JSONArray array) {
            items = array;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return items.length();
        }

        @Override
        public String getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;


            if (convertView == null) {
                holder = new ViewHolder();

                convertView = mInflater.inflate(R.layout.list_item_business, null);

                holder.imageView = (ImageView) convertView.findViewById(R.id.ivAvatar);
                holder.lbBusinesName = (CustomFontTextView)convertView.findViewById(R.id.tvBusinessName);
                holder.lbBusinesAddress = (CustomFontTextView)convertView.findViewById(R.id.tvBusinessAddress);
                holder.lbBusinesCategory = (CustomFontTextView)convertView.findViewById(R.id.tvBusinessCategory);
                holder.ivLikes = (ImageView) convertView.findViewById(R.id.ivLike);
                holder.lbLikes = (CustomFontTextView)convertView.findViewById(R.id.tvLike);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }


            try {

                JSONObject item = items.getJSONObject(position);


                try {
                    String url = Global.getURLEncoded(item.getString("LogoImageThumb"));

                    ImageLoader.getInstance().displayImage(url, holder.imageView);

                } catch (Exception e) {e.printStackTrace();}

                try {

                    holder.lbBusinesName.setText(item.getString("BusinessName"));
                    holder.lbBusinesAddress.setText(item.getString("BusinessAddressUserInput"));
                    holder.lbBusinesCategory.setText(item.getString("CategoryName"));
                } catch (Exception e) {e.printStackTrace();}


                if (AppData.getInstance().isBusiness()) {
                    holder.ivLikes.setVisibility(View.GONE);
                    holder.lbLikes.setVisibility(View.GONE);
                }
                else {
                    holder.ivLikes.setVisibility(View.VISIBLE);
                    holder.lbLikes.setVisibility(View.VISIBLE);

                    boolean liked = false;
                    int likes = 0;
                    try {
                        liked = item.getBoolean("CurrentUserLikesBusiness");
                        likes = item.getInt("Likes");
                    } catch (Exception e) {e.printStackTrace();}
                    holder.ivLikes.setImageResource(liked ? R.drawable.like_button_tapped : R.drawable.like_button);
                    holder.lbLikes.setText("" + likes);
                }

            } catch (Exception e) {e.printStackTrace();}



            return convertView;
        }

    }

    public static class ViewHolder {

        public ImageView imageView;
        public CustomFontTextView lbBusinesName;
        public CustomFontTextView lbBusinesAddress;
        public CustomFontTextView lbBusinesCategory;

        public ImageView ivLikes;
        public CustomFontTextView lbLikes;

    }

}
