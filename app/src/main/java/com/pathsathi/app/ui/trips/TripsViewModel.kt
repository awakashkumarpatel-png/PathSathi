package com.pathsathi.app.ui.trips
import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.pathsathi.app.PathSathiApp
import com.pathsathi.app.alerts.AlertScheduler
import com.pathsathi.app.data.db.TripEntity
import com.pathsathi.app.engine.*
import com.pathsathi.app.map.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
class TripsViewModel(app:Application):AndroidViewModel(app){private val repo=(app as PathSathiApp).repository;private val client=LocationServices.getFusedLocationProviderClient(app);private var cb:LocationCallback?=null;private val _trips=MutableStateFlow<List<TripEntity>>(emptyList());val trips:StateFlow<List<TripEntity>> = _trips;private val _guidance=MutableStateFlow("");val guidance:StateFlow<String> = _guidance;private val _distance=MutableStateFlow<Double?>(null);val distanceKm:StateFlow<Double?> = _distance;private val _eta=MutableStateFlow<Int?>(null);val etaMinutes:StateFlow<Int?> = _eta;private val _tracking=MutableStateFlow(false);val tracking:StateFlow<Boolean> = _tracking
 init{viewModelScope.launch{repo.observeTrips().collect{_trips.value=it}}}
 fun startTrip(t:TripEntity)=viewModelScope.launch{val now=System.currentTimeMillis();repo.observeTrips().first().filter{it.status=="ACTIVE"&&it.id!=t.id}.forEach{repo.updateTrip(it.copy(status="COMPLETED",completedAtEpochMs=now))};repo.updateTrip(t.copy(status="ACTIVE",startedAtEpochMs=now,currentDayIndex=0));AlertScheduler.scheduleReminder(getApplication(),"Trip started","Sathi is now tracking your trip to ${t.destination}.",1)}
 fun completeTrip(t:TripEntity){stopAutoTracking();viewModelScope.launch{repo.updateTrip(t.copy(status="COMPLETED",completedAtEpochMs=System.currentTimeMillis()))}}
 fun advanceDay(t:TripEntity)=viewModelScope.launch{val d=ItinerarySerializer.decode(t.itineraryJson);repo.updateTrip(t.copy(currentDayIndex=(t.currentDayIndex+1).coerceAtMost((d.size-1).coerceAtLeast(0))))}
 fun startAutoTracking(c:Context,t:TripEntity){if(ContextCompat.checkSelfPermission(c,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&ContextCompat.checkSelfPermission(c,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return;if(cb!=null)return;val req=LocationRequest.Builder(10000L).setMinUpdateIntervalMillis(5000L).setPriority(Priority.PRIORITY_HIGH_ACCURACY).build();cb=object:LocationCallback(){override fun onLocationResult(r:LocationResult){r.lastLocation?.let{refreshGuidance(t.id,it.latitude,it.longitude)}}};try{client.requestLocationUpdates(req,cb!!,c.mainLooper);_tracking.value=true}catch(_:SecurityException){_tracking.value=false}}
 fun stopAutoTracking(){cb?.let{client.removeLocationUpdates(it)};cb=null;_tracking.value=false}
 fun refreshGuidance(t:TripEntity)=viewModelScope.launch{refresh(t.id,null,null)}
 private fun refresh(id:Long,lat:Double?,lng:Double?)=viewModelScope.launch{val t=repo.getTrip(id)?:return@launch;val d=ItinerarySerializer.decode(t.itineraryJson);val idx=t.currentDayIndex.coerceIn(0,(d.size-1).coerceAtLeast(0));val day=d.getOrNull(idx);val spent=repo.observeTotalSpent(id).first();val per=if(t.days>0)t.budgetInr/t.days else t.budgetInr;val actual=if(t.startedAtEpochMs!=null)((System.currentTimeMillis()-t.startedAtEpochMs!!)/60000L).toInt().coerceAtLeast(0)else 0;val auto=(actual/480).coerceIn(0,(d.size-1).coerceAtLeast(0));if(t.status=="ACTIVE"&&auto>t.currentDayIndex)repo.updateTrip(t.copy(currentDayIndex=auto));val dest=day?.places?.firstOrNull()?:t.destination;val saved=repo.observeSavedPlaces().first().firstOrNull{it.name.equals(dest,true)&&it.lat!=null&&it.lng!=null};val target=if(saved!=null)GeoPoint(saved.lat!!,saved.lng!!)else GeoUtils.geocode(getApplication(),dest);val e=if(lat!=null&&lng!=null&&target!=null)GeoUtils.estimate(GeoPoint(lat,lng),target,TravelMode.LOCAL_TRANSIT)else null;_distance.value=e?.distanceKm;_eta.value=e?.etaMinutes;_guidance.value=AutoTravelEngine.evaluate(AutoTravelEngine.ProgressInput(idx+1,t.days,idx*480,actual,per,spent,dest,e?.distanceKm))}
 private fun refreshGuidance(id:Long,lat:Double,lng:Double)=refresh(id,lat,lng)
 override fun onCleared(){stopAutoTracking();super.onCleared()}}
