package com.chen.mingkai.flashgallery.View;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.chen.mingkai.flashgallery.Model.Photo;
import com.chen.mingkai.flashgallery.Model.PhotoCenter;
import com.chen.mingkai.flashgallery.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PhotoListFragment extends Fragment {
    // data models
    private static final String SAVED_SUBTITLE_VISIBLE = "subtitle";
    private static final int REQUEST_PHOTO = 1;
    private boolean mSubtitleVisible;
    private Callbacks mCallbacks; // controls callback to include list to masterdetail when available

    // views
    private RecyclerView mPhotoRecyclerView;
    private PhotoAdapter mAdapter;

    public interface Callbacks {
        void onPhotoSelected(Photo photo);
    }

    // inner classes
    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private Photo mPhoto;
        private TextView mTitleTextView;
        private TextView mDateTextView;
        private Button mDeleteBtn;

        public PhotoHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mTitleTextView = (TextView) itemView.findViewById(R.id.list_item_photo_title_text_view);
            mDateTextView = (TextView) itemView.findViewById(R.id.list_item_photo_date_text_view);
            mDeleteBtn = (Button) itemView.findViewById(R.id.list_item_delete_photo_btn);
        }

        public void bindPhoto(Photo photo) {
            mPhoto = photo;
            mTitleTextView.setText(mPhoto.getTitle());
            mDateTextView.setText(mPhoto.getDate().toString());
            mDeleteBtn.setOnClickListener((View view)->{
                PhotoCenter.get(getActivity()).deletePhoto(photo);
                updateUI();
            });
        }

        @Override
        public void onClick(View v) {
            mCallbacks.onPhotoSelected(mPhoto);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<Photo> mPhotos;

        public PhotoAdapter(List<Photo> photos) {
            mPhotos = photos;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.list_item_photo, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            Photo photo = mPhotos.get(position);
            holder.bindPhoto(photo);
        }

        @Override
        public int getItemCount() {
            return mPhotos.size();
        }

        public void setPhotos(List<Photo> photos) {
            mPhotos = photos;
        }
    }

    private class HttpRequestTask extends AsyncTask<Void, Void, String> {
        protected String getASCIIContentFromEntity(HttpEntity entity) throws IllegalStateException, IOException {
            InputStream in = entity.getContent();

            StringBuffer out = new StringBuffer();
            int n = 1;
            while (n > 0) {
                byte[] b = new byte[4096];
                n = in.read(b);
                if (n > 0)
                    out.append(new String(b, 0, n));
            }

            return out.toString();
        }

        @Override
        protected String doInBackground(Void... params) {
            final String url = "https://node-quick-gallery-mkaichen.c9.io/images";
            String img = null;

            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            HttpGet httpGet = new HttpGet(url);
            try {
                HttpResponse response = httpClient.execute(httpGet, localContext);
                HttpEntity entity = response.getEntity();
                img = getASCIIContentFromEntity(entity);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return img;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    // implemented list UI methods
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saveInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_list, container, false);
        mPhotoRecyclerView = (RecyclerView) view.findViewById(R.id.photo_recycler_view);

        mPhotoRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        if (null != saveInstanceState) {
            mSubtitleVisible = saveInstanceState.getBoolean(SAVED_SUBTITLE_VISIBLE);
        }

        updateUI();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_SUBTITLE_VISIBLE, mSubtitleVisible);
    }

    public void updateUI() {
        PhotoCenter photoCenter = PhotoCenter.get(getActivity());

        List<Photo> photos = photoCenter.getPhotos();

        if (null == mAdapter) {
            mAdapter = new PhotoAdapter(photos);
            mPhotoRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.setPhotos(photos);
            mAdapter.notifyDataSetChanged();
        }
        updateSubtitle();
    }

    // top menu methods
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_list, menu);

        // subtitle on menu
        MenuItem subtitleItem = menu.findItem(R.id.menu_item_show_subtitle);
        if (mSubtitleVisible) {
            subtitleItem.setTitle(R.string.hide_subtitle);
        } else {
            subtitleItem.setTitle(R.string.show_subtitle);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean itemSelected = true;
        switch (item.getItemId()) {
            case R.id.menu_item_refresh:
                Toast.makeText(getActivity(), "Refreshing", Toast.LENGTH_SHORT).show();

                String data = null;
                // get RESTful from here
                new HttpRequestTask().execute();

                break;
            case R.id.menu_item_new_photo:
                Photo photo = new Photo();
                PhotoCenter.get(getActivity()).addPhoto(photo);
                updateUI();
                mCallbacks.onPhotoSelected(photo);
                break;
            case R.id.menu_item_show_subtitle:
                mSubtitleVisible = !mSubtitleVisible; // toggle
                getActivity().invalidateOptionsMenu();
                updateSubtitle(); // subtitle availability is checked then turned on/off
                break;
            default:
                itemSelected = super.onOptionsItemSelected(item);
        }
        return itemSelected;
    }

    private void updateSubtitle() {
        PhotoCenter photoCenter = PhotoCenter.get(getActivity());
        int photoCount = photoCenter.getPhotos().size();
        String subtitle = getString(R.string.subtitle_format, photoCount);

        if (false == mSubtitleVisible) {
            subtitle = null;
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setSubtitle(subtitle);
    }
}