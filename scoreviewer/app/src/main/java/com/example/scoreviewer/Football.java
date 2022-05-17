package com.example.scoreviewer;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.scoreviewer.ble.peripheral.Communications;


public class Football extends Fragment {
    private static final String TAG = Football.class.getSimpleName();

    // UI TextBuffer (refreshing the text buffer is managed with a timer because a lot of changes can arrive really fast and could stall the main thread)
    private Handler mUIRefreshTimerHandler = new Handler();
    private Runnable mUIRefreshTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isUITimerRunning) {
                updateTextDataUI();
                // Log.d(TAG, "updateDataUI");
                mUIRefreshTimerHandler.postDelayed(this, 200);
            }
        }
    };

    private boolean isUITimerRunning = false;

    private Communications mCommunications;

    Button TouchdownT1;
    Button ExtraPointT1;
    Button RemoveTouchdownT1;
    Button RemoveExtrapointT1;

    Button TouchdownT2;
    Button ExtraPointT2;
    Button RemoveTouchdownT2;
    Button RemoveExtrapointT2;

    Button ResetScores;
    // UI
    private TextView mT1ScoreView;
    private TextView mT2ScoreView;

    private Football.FootballListener mListener;
    private volatile StringBuilder mDataBuffer = new StringBuilder();
    View.OnTouchListener mPadButtonTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            final int tag = Integer.valueOf((String) view.getTag());
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                view.setPressed(true);
                mListener.onSendControllerPadButtonStatus(tag, true);
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                view.setPressed(false);
                mListener.onSendControllerPadButtonStatus(tag, false);
                view.performClick();
                return true;
            }
            return false;
        }
    };
    // region Lifecycle
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Football newInstance(Communications _mCommunications) {
        Football fragment = new Football(_mCommunications);
        return fragment;
    }

    public Football(Communications _mCommunications) {
        mCommunications=_mCommunications;
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_football, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)  {
        super.onViewCreated(view, savedInstanceState);

        // Set title
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(R.string.gamefootball);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        TouchdownT1=view.findViewById(R.id.TouchdownTeam1);
        ExtraPointT1=view.findViewById(R.id.ExtraPointTeam1);
        RemoveTouchdownT1=view.findViewById(R.id.RemoveTouchdownTeam1);
        RemoveExtrapointT1=view.findViewById(R.id.RemoveExtraPointTeam1);


        TouchdownT2=view.findViewById(R.id.TouchdownTeam2);
        ExtraPointT2=view.findViewById(R.id.ExtraPointTeam2);
        RemoveTouchdownT2=view.findViewById(R.id.RemoveTouchdownTeam2);
        RemoveExtrapointT2=view.findViewById(R.id.RemoveExtraPointTeam2);


        ResetScores=view.findViewById(R.id.FB_resetscores);

        mT1ScoreView=view.findViewById(R.id.Team1ScoreFB);
        mT2ScoreView=view.findViewById(R.id.Team2ScoreFB);

        mCommunications.sendScoreUpdate(Communications.requestScoreupdate);
        mT1ScoreView.setText(String.format(getString(R.string.team_score_format), mCommunications.getT1Score()));
        mT2ScoreView.setText(String.format(getString(R.string.team_score_format), mCommunications.getT2Score()));


        Log.d(TAG, "Ready");

        TouchdownT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<6;i++)
                    mCommunications.sendScoreUpdate(mCommunications.addT1);
            }
        });

        ExtraPointT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<1;i++)
                    mCommunications.sendScoreUpdate(mCommunications.addT1);
            }
        });

        RemoveTouchdownT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<6;i++)
                    mCommunications.sendScoreUpdate(mCommunications.subT1);
            }
        });
        RemoveExtrapointT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<1;i++)
                    mCommunications.sendScoreUpdate(mCommunications.subT1);
            }
        });

        TouchdownT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<6;i++)
                    mCommunications.sendScoreUpdate(mCommunications.addT2);
            }
        });
        ExtraPointT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<1;i++)
                    mCommunications.sendScoreUpdate(mCommunications.addT2);
            }
        });
        RemoveTouchdownT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<6;i++)
                    mCommunications.sendScoreUpdate(mCommunications.subT2);
            }
        });
        RemoveExtrapointT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<1;i++)
                    mCommunications.sendScoreUpdate(mCommunications.subT2);
            }
        });


        ResetScores.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCommunications.sendScoreUpdate(mCommunications.resetAll);
            }
        });
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Football.FootballListener) {
            mListener = (Football.FootballListener) context;
        } else if (getTargetFragment() instanceof Football.FootballListener) {
            mListener = (Football.FootballListener) getTargetFragment();
        } else {
            throw new RuntimeException(context.toString() + " must implement FootballListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh timer
        isUITimerRunning = true;
        mUIRefreshTimerHandler.postDelayed(mUIRefreshTimerRunnable, 0);
    }

    @Override
    public void onPause() {
        super.onPause();
        isUITimerRunning = false;
        mUIRefreshTimerHandler.removeCallbacksAndMessages(mUIRefreshTimerRunnable);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_help, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        FragmentActivity activity = getActivity();

        switch (item.getItemId()) {
            case R.id.action_help:
                if (activity != null) {
                    FragmentManager fragmentManager = activity.getSupportFragmentManager();
                    if (fragmentManager != null) {
                        CommonHelpFragment helpFragment = CommonHelpFragment.newInstance(getString(R.string.controlpad_help_title), getString(R.string.controlpad_help_text));
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction()
                                .replace(R.id.contentLayout, helpFragment, "Help");
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                    }
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // endregion
    // region UI
    private synchronized void updateTextDataUI() {
        mT1ScoreView.setText(String.format(getString(R.string.team_score_format), mCommunications.getT1Score()));
        mT2ScoreView.setText(String.format(getString(R.string.team_score_format), mCommunications.getT2Score()));
    }
    // endregion

    // region CornholeListener
    public interface FootballListener {
        void onSendControllerPadButtonStatus(int tag, boolean isPressed);
    }
    // endregion
}
