package io.mrarm.irc;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.irc.util.ImageViewTintUtils;

public class ChatFragment extends Fragment {

    private static final String ARG_SERVER_UUID = "server_uuid";
    private static final String ARG_CHANNEL_NAME = "channel";

    private ServerConnectionInfo mConnectionInfo;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private ChannelMembersAdapter mChannelMembersAdapter;
    private EditText mSendText;
    private ImageView mSendIcon;

    public static ChatFragment newInstance(ServerConnectionInfo server, String channel) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SERVER_UUID, server.getUUID().toString());
        if (channel != null)
            args.putString(ARG_CHANNEL_NAME, channel);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat_content, container, false);

        UUID connectionUUID = UUID.fromString(getArguments().getString(ARG_SERVER_UUID));
        mConnectionInfo = ServerConnectionManager.getInstance().getConnection(connectionUUID);
        String requestedChannel = getArguments().getString(ARG_CHANNEL_NAME);

        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(mConnectionInfo.getName());

        ((MainActivity) getActivity()).addActionBarDrawerToggle(toolbar);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager(), mConnectionInfo);

        mViewPager = (ViewPager) rootView.findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        if (requestedChannel != null)
            setCurrentChannel(requestedChannel);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) { }

            @Override
            public void onPageSelected(int i) {
                ((MainActivity) getActivity()).getDrawerHelper().setSelectedChannel(mConnectionInfo,
                        mSectionsPagerAdapter.getChannel(i));
            }

            @Override
            public void onPageScrollStateChanged(int i) { }
        });

        mConnectionInfo.addOnChannelListChangeListener((ServerConnectionInfo connection,
                                                        List<String> newChannels) -> {
            getActivity().runOnUiThread(() -> {
                mSectionsPagerAdapter.notifyDataSetChanged();
            });
        });

        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        mChannelMembersAdapter = new ChannelMembersAdapter(null);
        RecyclerView membersRecyclerView = (RecyclerView) rootView.findViewById(R.id.members_list);
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        membersRecyclerView.setAdapter(mChannelMembersAdapter);

        mSendText = (EditText) rootView.findViewById(R.id.send_text);
        mSendIcon = (ImageButton) rootView.findViewById(R.id.send_button);

        ImageViewTintUtils.setTint(mSendIcon, 0x54000000);

        mSendText.addTextChangedListener(new TextWatcher() {
            boolean wasEmpty = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isEmpty = (s.length() > 0);
                if (isEmpty == wasEmpty)
                    return;
                wasEmpty = isEmpty;
                int accentColor = getResources().getColor(R.color.colorAccent);
                if (s.length() > 0)
                    ImageViewTintUtils.setTint(mSendIcon, accentColor);
                else
                    ImageViewTintUtils.setTint(mSendIcon, 0x54000000);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mSendIcon.setOnClickListener((View view) -> {
            String text = mSendText.getText().toString();
            mConnectionInfo.getApiInstance().sendMessage(mSectionsPagerAdapter.getChannel(
                    mViewPager.getCurrentItem()), text, null, null);
            mSendText.setText("");
        });

        return rootView;
    }

    public ServerConnectionInfo getConnectionInfo() {
        return mConnectionInfo;
    }

    public void setCurrentChannel(String channel) {
        mViewPager.setCurrentItem(mConnectionInfo.getChannels().indexOf(channel) + 1);
    }

    public void setCurrentChannelMembers(List<NickWithPrefix> members) {
        mChannelMembersAdapter.setMembers(members);
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private ServerConnectionInfo connectionInfo;

        public SectionsPagerAdapter(FragmentManager fm, ServerConnectionInfo connectionInfo) {
            super(fm);
            this.connectionInfo = connectionInfo;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0)
                return MessagesFragment.newStatusInstance(connectionInfo);
            return MessagesFragment.newInstance(connectionInfo,
                    connectionInfo.getChannels().get(position - 1));
        }

        @Override
        public int getCount() {
            if (connectionInfo.getChannels() == null)
                return 1;
            return connectionInfo.getChannels().size() + 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0)
                return getString(R.string.tab_server);
            return connectionInfo.getChannels().get(position - 1);
        }

        public String getChannel(int position) {
            if (position == 0)
                return null;
            return connectionInfo.getChannels().get(position - 1);
        }

    }
}