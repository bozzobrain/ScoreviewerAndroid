package com.example.scoreviewer;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.controls.Control;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scoreviewer.ble.BleUtils;
import com.example.scoreviewer.ble.central.BlePeripheral;
import com.example.scoreviewer.ble.central.BlePeripheralBattery;
import com.example.scoreviewer.ble.central.BlePeripheralDfu;
import com.example.scoreviewer.ble.central.BlePeripheralUart;
import com.example.scoreviewer.ble.central.BleScanner;
import com.example.scoreviewer.ble.central.UartDataManager;
import com.example.scoreviewer.ble.peripheral.Communications;
import com.example.scoreviewer.style.RssiUI;
import com.example.scoreviewer.utils.DialogUtils;
import com.example.scoreviewer.utils.LocalizationManager;

import com.example.scoreviewer.Cornhole;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Controlling extends ConnectedPeripheralFragment implements Cornhole.CornholeListener, Football.FootballListener, Volleyball.VolleyballListener, Kickball.KickballListener, UartDataManager.UartDataManagerListener {
    private final static String TAG = Controlling.class.getSimpleName();

    // Fragment parameters
    private final static int CONNECTIONMODE_SINGLEPERIPHERAL = 0;
    private final static int CONNECTIONMODE_MULTIPLEPERIPHERAL = 1;

    // Constants
    private final static int MODULE_INFO = 0;
    private final static int MODULE_CORNHOLE = 1;
    private final static int MODULE_FOOTBALL = 2;
    private final static int MODULE_VOLLEYBALL = 3;
    private final static int MODULE_KICKBALL = 4;

    private final static int NUMBER_OF_GAMES = 5;

    Communications mCommunications = new Communications();



    // Data
    private ControllingListener mListener;
    private List<BlePeripheralBattery> mBatteryPeripherals = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private ModulesAdapter mModulesAdapter;
    private BlePeripheralUart mBlePeripheralUart;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    // region Fragment Lifecycle
    public static Controlling newInstance(@Nullable String singlePeripheralIdentifier) {      // if singlePeripheralIdentifier is null, uses multi-connect
        Controlling fragment = new Controlling();
        fragment.setArguments(createFragmentArgs(singlePeripheralIdentifier));
        return fragment;
    }

    public Controlling() {
        // Required empty public constructor
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mListener = (ControllingListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ControllingListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_peripheralmodules, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Setup communications tunnel
        mCommunications.initCommunications(mBlePeripheral);

        // Update ActionBar
        setActionBarTitle(R.string.peripheralmodules_title);

        final Context context = getContext();
        if (context != null) {
            // Peripherals recycler view
            mRecyclerView = view.findViewById(R.id.recyclerView);
            DividerItemDecoration itemDecoration = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
            Drawable lineSeparatorDrawable = ContextCompat.getDrawable(context, R.drawable.simpledivideritemdecoration);
            assert lineSeparatorDrawable != null;
            itemDecoration.setDrawable(lineSeparatorDrawable);
            mRecyclerView.addItemDecoration(itemDecoration);

            mRecyclerView.setHasFixedSize(false);
            RecyclerView.LayoutManager mPeripheralsLayoutManager = new LinearLayoutManager(getContext());
            mRecyclerView.setLayoutManager(mPeripheralsLayoutManager);
        }

        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();        // update options menu with current values
        }
        if (mUartDataManager == null) { // Only the first time
            // Google Play Services (used for location updates)
            //buildGoogleApiClient(context);

            // Setup
            mUartDataManager = new UartDataManager(context, null, false);            // The listener will be set only for ControlPad
            setupUart();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final Context context = getContext();
        if (context == null) {
            Log.w(TAG, "onResume with null context");
            return;
        }

        // Start reading battery
        mBatteryPeripherals.clear();
        if (mBlePeripheral != null) {   // Single peripheral
            startBatteryUI(mBlePeripheral);
        } else {       // Multiple peripherals
            List<BlePeripheral> connectedPeripherals = BleScanner.getInstance().getConnectedPeripherals();
            for (BlePeripheral blePeripheral : connectedPeripherals) {
                startBatteryUI(blePeripheral);
            }
        }

        // Setup
        WeakReference<Controlling> weakFragment = new WeakReference<>(this);
        mModulesAdapter = new Controlling.ModulesAdapter(context, mBatteryPeripherals, mBlePeripheral, view1 -> {
            Controlling fragment = weakFragment.get();
            if (fragment != null) {
                final int moduleId = (int) view1.getTag();
                fragment.onModuleSelected(moduleId);
            }
        });
        mRecyclerView.setAdapter(mModulesAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();

        //  Disable notification for battery reading
        for (BlePeripheralBattery blePeripheralBattery : mBatteryPeripherals) {
            blePeripheralBattery.stopReadingBatteryLevel(null);
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Null out references to views to avoid leaks when the fragment is added to the backstack: https://stackoverflow.com/questions/59503689/could-navigation-arch-component-create-a-false-positive-memory-leak/59504797#59504797
        mRecyclerView = null;
        mModulesAdapter = null;
    }

    // endregion

    // region Battery
    private void startBatteryUI(@NonNull BlePeripheral blePeripheral) {
        final boolean hasBattery = BlePeripheralBattery.hasBattery(blePeripheral);

        if (hasBattery) {
            BlePeripheralBattery blePeripheralBattery = new BlePeripheralBattery(blePeripheral);
            final int batteryIndex = mBatteryPeripherals.size();
            mBatteryPeripherals.add(blePeripheralBattery);

            blePeripheralBattery.startReadingBatteryLevel(level -> {
                if (mModulesAdapter != null) {
                    Log.d(TAG, "onBatteryLevelChanged: " + level + " for index: " + batteryIndex);

                    final Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(() -> mModulesAdapter.notifyDataSetChanged());
                }
            });
        }
    }

    // endregion

    // region Actions

    private UartDataManager mUartDataManager;
    private WeakReference<Cornhole> mWeakCornhole = null;
    private WeakReference<Football> mWeakFootball = null;
    private WeakReference<Kickball> mWeakKickball = null;
    private WeakReference<Volleyball> mWeakVolleyball = null;

    private void onModuleSelected(int moduleId) {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            FragmentManager fragmentManager = activity.getSupportFragmentManager();

            Fragment fragment = null;
            String fragmentTag = null;
            switch (moduleId) {
                case MODULE_INFO:
                    fragment = InfoFragment.newInstance(null);
                break;
                case MODULE_CORNHOLE:
                    Cornhole cornhole = Cornhole.newInstance(mCommunications);
                    fragment = cornhole;
                    fragmentTag = "Cornhole";

                    // Enable cache for control pad
                    mWeakCornhole = new WeakReference<>(cornhole);
                    mUartDataManager.clearRxCache(mBlePeripheral.getIdentifier());
                    mUartDataManager.setListener(Controlling.this);
                    break;

                case MODULE_FOOTBALL:
                    Football football = Football.newInstance(mCommunications);
                    fragment = football;
                    fragmentTag = "Football";

                    // Enable cache for control pad
                    mWeakFootball = new WeakReference<>(football);
                    mUartDataManager.clearRxCache(mBlePeripheral.getIdentifier());
                    mUartDataManager.setListener(Controlling.this);
                    break;
                case MODULE_KICKBALL:
                    Kickball kickball = Kickball.newInstance(mCommunications);
                    fragment = kickball;
                    fragmentTag = "Kickball";

                    // Enable cache for control pad
                    mWeakKickball = new WeakReference<>(kickball);
                    mUartDataManager.clearRxCache(mBlePeripheral.getIdentifier());
                    mUartDataManager.setListener(Controlling.this);
                    break;
                case MODULE_VOLLEYBALL:
                    Volleyball volleyball = Volleyball.newInstance(mCommunications);
                    fragment = volleyball;
                    fragmentTag = "Volleyball";

                    // Enable cache for control pad
                    mWeakVolleyball = new WeakReference<>(volleyball);
                    mUartDataManager.clearRxCache(mBlePeripheral.getIdentifier());
                    mUartDataManager.setListener(Controlling.this);
                    break;
            }

            if (fragment != null) {
                fragment.setTargetFragment(Controlling.this, 0);
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left)
                        .replace(R.id.contentLayout, fragment, fragmentTag);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commitAllowingStateLoss();      // Allowing state loss to avoid detected crashes
            }
        }
    }


    // region UartDataManagerListener

    @Override
    public void onUartRx(@NonNull byte[] data, @Nullable String peripheralIdentifier) {
            String dataString = BleUtils.bytesToText(data, true);
            mCommunications.addText(dataString);
            mUartDataManager.removeRxCacheFirst(data.length, peripheralIdentifier);
    }

    // endregion

    // region Listeners
    interface ControllingListener {
        void startModuleFragment(Fragment fragment);
    }

    // endregion


    // region Adapter
    private static class ModulesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        // Constants
        private static final int kCellType_SectionTitle = 0;
        private static final int kCellType_PeripheralDetails = 1;
        private static final int kCellType_Module = 2;

        private static final int kPeripheralDetailsCellsStartPosition = 1;

        // Data
        private Context mContext;
        private List<BlePeripheralBattery> mBatteryPeripherals;
        private int mConnectionMode;
        private List<BlePeripheral> mConnectedPeripherals;
        private BlePeripheral mBlePeripheral;
        private View.OnClickListener mOnClickListener;

        ModulesAdapter(@NonNull Context context, @NonNull List<BlePeripheralBattery> batteryPeripherals, @Nullable BlePeripheral blePeripheralForSingleConnectionMode, @NonNull View.OnClickListener onClickListener) {
            mContext = context.getApplicationContext();
            mBatteryPeripherals = batteryPeripherals;
            mConnectionMode =  CONNECTIONMODE_SINGLEPERIPHERAL;
            mBlePeripheral = blePeripheralForSingleConnectionMode;
            mOnClickListener = onClickListener;
        }

        private int getModuleCellsStartPosition() {
            return kPeripheralDetailsCellsStartPosition + mConnectedPeripherals.size() + 1;  // +1 because Modules header
        }

        @Override
        public int getItemViewType(int position) {
            super.getItemViewType(position);

            final int kModulesSectionTitlePosition = getModuleCellsStartPosition() - 1;
            if (position == kPeripheralDetailsCellsStartPosition - 1 || position == kModulesSectionTitlePosition) {
                return kCellType_SectionTitle;
            } else if (position < kModulesSectionTitlePosition) {
                return kCellType_PeripheralDetails;
            } else {
                return kCellType_Module;
            }
        }

        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            //Log.d(TAG, "onCreateViewHolder type: " + viewType);
            switch (viewType) {
                case kCellType_SectionTitle: {
                    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_common_section_item, parent, false);
                    return new Controlling.ModulesAdapter.SectionViewHolder(view);
                }
                case kCellType_PeripheralDetails: {
                    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_peripheralmodules_peripheraldetails, parent, false);
                    return new Controlling.ModulesAdapter.PeripheralDetailsViewHolder(view);
                }
                case kCellType_Module: {
                    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_peripheralmodules_module, parent, false);
                    return new Controlling.ModulesAdapter.ModuleViewHolder(view);
                }
                default: {
                    Log.e(TAG, "Unknown cell type");
                    throw new AssertionError("Unknown cell type");
                }
            }
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {

            LocalizationManager localizationManager = LocalizationManager.getInstance();
            final int viewType = getItemViewType(position);
            switch (viewType) {
                case kCellType_SectionTitle: {
                    String stringId;
//                    stringId = "controlling_sectiontitle_games";
                    if (position == kPeripheralDetailsCellsStartPosition - 1) {
                        stringId = "peripheralmodules_sectiontitle_device_single";
                    } else {
                        stringId = "controlling_sectiontitle_games";
                    }

                    Controlling.ModulesAdapter.SectionViewHolder sectionViewHolder = (Controlling.ModulesAdapter.SectionViewHolder) holder;
                    sectionViewHolder.titleTextView.setText(localizationManager.getString(mContext, stringId));
                    break;
                }

                case kCellType_PeripheralDetails: {
                    final int detailsIndex = position - kPeripheralDetailsCellsStartPosition;
                    BlePeripheral blePeripheral = mConnectedPeripherals.get(detailsIndex);

                    String name = blePeripheral.getName();
                    if (name == null) {
                        name = mContext.getString(R.string.scanner_unnamed);
                    }

                    Controlling.ModulesAdapter.PeripheralDetailsViewHolder detailsViewHolder = (Controlling.ModulesAdapter.PeripheralDetailsViewHolder) holder;
                    detailsViewHolder.nameTextView.setText(name);
                    final int rssi = blePeripheral.getRssi();
                    detailsViewHolder.rssiImageView.setImageResource(RssiUI.getDrawableIdForRssi(rssi));
                    detailsViewHolder.rssiTextView.setText(String.format(Locale.ENGLISH, mContext.getString(R.string.peripheralmodules_rssi_format), rssi));

                    BlePeripheralBattery blePeripheralBattery = getPeripheralBatteryForPeripheral(blePeripheral);
                    final boolean hasBattery = blePeripheralBattery != null && blePeripheralBattery.getCurrentBatteryLevel() >= 0;      // if batter value is -1 means that is not available
                    detailsViewHolder.batteryGroupView.setVisibility(hasBattery ? View.VISIBLE : View.GONE);
                    if (hasBattery) {
                        final int batteryLevel = blePeripheralBattery.getCurrentBatteryLevel();
                        final String batteryPercentage = String.format(mContext.getString(R.string.peripheralmodules_battery_format), batteryLevel);
                        detailsViewHolder.batteryTextView.setText(batteryPercentage);
                    }

                    break;
                }

                case kCellType_Module: {
                    final int moduleIndex = position - getModuleCellsStartPosition();
                    final int moduleId = getMenuItems()[moduleIndex];

                    int iconDrawableId = 0;
                    int titleId = 0;
                    switch (moduleId) {
                        case MODULE_INFO:
                            iconDrawableId = R.drawable.tab_info_icon;
                            titleId = R.string.info_tab_title;
                            break;

                        case MODULE_CORNHOLE:
                            iconDrawableId = R.drawable.cornhole;
                            titleId = R.string.gamecornhole;
                            break;

                        case MODULE_FOOTBALL:
                            iconDrawableId = R.drawable.football;
                            titleId = R.string.gamefootball;
                            break;

                        case MODULE_VOLLEYBALL:
                            iconDrawableId = R.drawable.volleyball;
                            titleId = R.string.gamevolleyball;
                            break;

                        case MODULE_KICKBALL:
                            iconDrawableId = R.drawable.kickball;
                            titleId = R.string.gamekickball;
                            break;
                    }

                    Controlling.ModulesAdapter.ModuleViewHolder moduleViewHolder = (Controlling.ModulesAdapter.ModuleViewHolder) holder;
                    if (iconDrawableId != 0) {
                        moduleViewHolder.iconImageView.setImageResource(iconDrawableId);
                    }
                    if (titleId != 0) {
                        moduleViewHolder.nameTextView.setText(titleId);
                    }

                    moduleViewHolder.mainViewGroup.setTag(moduleId);
                    moduleViewHolder.mainViewGroup.setOnClickListener(view -> mOnClickListener.onClick(view));
                    break;
                }
            }
        }

        @SuppressWarnings("UnnecessaryLocalVariable")
        @Override
        public int getItemCount() {
            final int kNumSections = 2;
            mConnectedPeripherals = BleScanner.getInstance().getConnectedPeripherals();
            final int numItems = kNumSections + mConnectedPeripherals.size() + getMenuItems().length;
            Log.d(TAG, "menuitems: "+getMenuItems().length);
            return numItems;
        }



        private int[] getMenuItems() {
                return new int[]{MODULE_INFO, MODULE_CORNHOLE, MODULE_FOOTBALL, MODULE_KICKBALL, MODULE_VOLLEYBALL};
        }

        class SectionViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView;

            SectionViewHolder(View view) {
                super(view);
                titleTextView = view.findViewById(R.id.titleTextView);
            }
        }

        class PeripheralDetailsViewHolder extends RecyclerView.ViewHolder {
            TextView nameTextView;
            ImageView rssiImageView;
            TextView rssiTextView;
            ViewGroup batteryGroupView;
            TextView batteryTextView;

            PeripheralDetailsViewHolder(View view) {
                super(view);
                nameTextView = view.findViewById(R.id.nameTextView);
                rssiImageView = view.findViewById(R.id.rssiImageView);
                rssiTextView = view.findViewById(R.id.rssiTextView);
                batteryGroupView = view.findViewById(R.id.batteryGroupView);
                batteryTextView = view.findViewById(R.id.batteryTextView);
            }
        }

        class ModuleViewHolder extends RecyclerView.ViewHolder {
            ViewGroup mainViewGroup;
            ImageView iconImageView;
            TextView nameTextView;

            ModuleViewHolder(View view) {
                super(view);
                mainViewGroup = view.findViewById(R.id.mainViewGroup);
                mainViewGroup.setClickable(true);
                iconImageView = view.findViewById(R.id.iconImageView);
                nameTextView = view.findViewById(R.id.nameTextView);
            }
        }

        private @Nullable
        BlePeripheralBattery getPeripheralBatteryForPeripheral(@NonNull BlePeripheral blePeripheral) {
            String identifier = blePeripheral.getIdentifier();
            BlePeripheralBattery result = null;

            boolean found = false;
            int i = 0;
            while (!found && i < mBatteryPeripherals.size()) {
                BlePeripheralBattery blePeripheralBattery = mBatteryPeripherals.get(i);
                if (blePeripheralBattery.getIdentifier().equals(identifier)) {
                    found = true;
                    result = blePeripheralBattery;
                }
                i++;
            }

            return result;
        }
    }

    // endregion

    // region Uart
    private void setupUart() {
        if (mBlePeripheral == null) {
            Log.e(TAG, "setupUart with blePeripheral null");
            return;
        }

        mBlePeripheralUart = new BlePeripheralUart(mBlePeripheral);
        mBlePeripheralUart.uartEnable(mUartDataManager, status -> mMainHandler.post(() -> {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Done
                Log.d(TAG, "Uart enabled");

            } else {
                Context context = getContext();
                if (context != null) {
                    WeakReference<BlePeripheralUart> weakBlePeripheralUart = new WeakReference<>(mBlePeripheralUart);
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    AlertDialog dialog = builder.setMessage(R.string.uart_error_peripheralinit)
                            .setPositiveButton(android.R.string.ok, (dialogInterface, which) -> {
                                BlePeripheralUart strongBlePeripheralUart = weakBlePeripheralUart.get();
                                if (strongBlePeripheralUart != null) {
                                    strongBlePeripheralUart.disconnect();
                                }
                            })
                            .show();
                    DialogUtils.keepDialogOnOrientationChanges(dialog);
                }
            }
        }));
    }

    // region ControllerPadFragmentListener
    @Override
    public void onSendControllerPadButtonStatus(int tag, boolean isPressed) {
        String data = "!B" + tag + (isPressed ? "1" : "0");
        ByteBuffer buffer = ByteBuffer.allocate(data.length()).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        buffer.put(data.getBytes());
    }
    // endregion
}
