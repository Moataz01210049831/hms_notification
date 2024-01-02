# hms_notification
send this using postman 
{
    "validate_only":false,
 "message": {
        "data": "{\"id\" : \"5\",\"titleAr\" : \"عري 3\",\"titleEn\" : \"desc2\",\"bodyAr\" : \"2البريد\",\"bodyEn\" : \"message\",\"type\" : \"announcement\",\"sound\" : \"/raw/ss\"}",
       
        "token": [
            "IQAAAACy03leAACYrONhXUZ5k0DDVwLEIL7r2xfHN50Jy1WGwdn8AKDwVur2s0A_-jg_Emi_RqokrkP4LglPi54X9sSNuh0a9DTlEpWACWCKAQRWEQ"
        ]
    }
}

#add this to service.ts

/* eslint-disable @angular-eslint/contextual-lifecycle */
/* eslint-disable @typescript-eslint/no-unused-expressions */
import { Injectable } from '@angular/core';
import { Component, OnInit } from '@angular/core';
import {
  ActionPerformed,
  PushNotificationSchema,
  PushNotifications,
  Token,
  NotificationChannel,
  Channel,
} from '@capacitor/push-notifications';
import { NavController, Platform } from '@ionic/angular';
import { select } from '@ngrx/store';
import { AuthService } from 'ionic-appauth';
import { Observable } from 'rxjs';
import { identitySelectors, selectIsAuthenticated } from 'src/app/app.state';
import { CommonServices } from 'src/app/common/common-services.service';
import { Announcement, notificationsActions, RegisterMobile } from 'src/app/pages/notifications/state';
import { CustomerProfileConnector } from 'src/app/pages/profile/customer-profile/customer-profile.connector';
import { LocalNotifications, ScheduleOptions } from '@capacitor/local-notifications';
import { environment } from 'src/environments/environment';

import { HmsLocalNotification, HmsPush, HmsPushEvent, } from '@hmscore/ionic-native-hms-push/ngx';

@Injectable({
  providedIn: 'root'
})
export class NotificationsService implements OnInit {
  registerMobile: RegisterMobile;
  isAuthenticated$: Observable<boolean>;
  isAuthenticated: boolean;
  announcements: Announcement[] = [];;

  constructor(
    public navCtrl: NavController,
    private commonServices: CommonServices,
    private auth: AuthService,
    private platform: Platform,
    private hmsPush: HmsPush,
    private hmsPushEvent: HmsPushEvent,
    private hmsLocal:HmsLocalNotification
  ) {
    
    this.platform.ready().then(()=>{
      this.commonServices.isHMSAndNotGMS().then(async isHms=>{
        if(isHms){
          this.hmsPush.init()
         
          this.turnOnPush();
          this.getToken();
         
          this.initListener();
          this.hmsPush.setBackgroundFile("public/assets/background.js");
        }
      });
     
    })
    
  }

  initListener(){
    console.log("hms events");
   
    try{
      this.hmsPushEvent.onRemoteMessageReceived((res)=>{
        console.log("ON_RECIEVED_MESSAGE", (res));
        console.log("ON_RECIEVED_MESSAGE", (res.msg.data));
        const title=JSON.parse(res.msg.data);
        console.log('help',JSON.parse(res.msg.data).titleAr);
        const notifcationId = (res.msg.id);
        const notAndroid = {
         
            id: notifcationId,
            message:this.commonServices.lang === 'ar' ? JSON.parse(res.msg.data).bodyAr : JSON.parse(res.msg.data).bodyEn,
            schedule: {at: new Date(Date.now())},
            title: this.commonServices.lang === 'ar' ? JSON.parse(res.msg.data).titleAr : JSON.parse(res.msg.data).titleEn,
            soundName: 'ss.wav',
            playSound:true,
           
           
          
        };
       this.hmsLocal.localNotification(notAndroid).then(data=>{
        console.log("ended data: ",data);
        if (title?.type === 'announcement') {
          const announcement = new Announcement();
          announcement.bodyAr = title?.bodyAr;
          announcement.bodyEn = title?.bodyEn;
          announcement.titleAr = title?.titleAr;
          announcement.titleEn = title?.titleEn;
          announcement.isRead = false;
          announcement.sound = title?.sound;
          announcement.type = title?.type;
          announcement.notificationDate = new Date();
          announcement.id=data.id;

          const allAnnouncements: any = localStorage.getItem('allAnnouncements');

          if (allAnnouncements && allAnnouncements !== 'undefined') {
            this.announcements = [];
            this.announcements = JSON.parse(allAnnouncements);
            this.announcements.push(announcement);
            localStorage.setItem('allAnnouncements', JSON.stringify(this.announcements));
          }
          else {
            this.announcements = [];
            this.announcements.push(announcement);
            localStorage.setItem('allAnnouncements', JSON.stringify(this.announcements));
          }
        }
       })
            
  
      });
      this.hmsPushEvent.onNotificationOpenedApp((result)=>{
        console.log("NOTIFICATION_OPENED_EVENT",JSON.stringify(result));
        console.log("NOTIFICATION_OPENED_EVENT",JSON.stringify(result.extras.notification?.notification));
        try{
          const dataObject = JSON.parse(result.extras.notification?.notification);
          if (dataObject?.type === 'announcement') {
            const announcement = new Announcement();
            announcement.bodyAr = dataObject?.bodyAr;
            announcement.bodyEn = dataObject?.bodyEn;
            announcement.titleAr = dataObject?.titleAr;
            announcement.titleEn = dataObject?.titleEn;
            announcement.isRead = false;
            announcement.sound = dataObject?.sound;
            announcement.type = dataObject?.type;
            announcement.notificationDate = new Date();
            announcement.id=dataObject?.id;
  
            const allAnnouncements: any = localStorage.getItem('allAnnouncements');
            console.log("NOTIFICATION_OPENED_EVENT0",JSON.stringify(allAnnouncements));
            if (allAnnouncements && allAnnouncements !== 'undefined' ) {
              this.announcements = [];
              this.announcements = JSON.parse(allAnnouncements);
              console.log("NOTIFICATION_OPENED_EVENT1",JSON.stringify(announcement));
              this.announcements.push(announcement);
              localStorage.setItem('allAnnouncements', JSON.stringify(this.announcements));
            }
            else {
              console.log("NOTIFICATION_OPENED_EVENT2",JSON.stringify(announcement));
              this.announcements = [];
              this.announcements.push(announcement);
              localStorage.setItem('allAnnouncements', JSON.stringify(this.announcements));
            }
  }
        }catch(r){
          console.log("ERROR READING JSON OBJECT")
        }
       
          
        
        if (result.extras.notification &&  result.remoteMessage.extras?.notification !== null){
          
           this.commonServices.router.navigateByUrl('/notifications/myNotifications/announcement');
        }
        
      });

      // this.hmsPushEvent.onLocalNotificationAction((result) => {
       
      //   const notification = JSON.parse(result.dataJSON);
      //   if (notification.action === "Yes") {
      //     this.hmsLocal.cancelNotificationsWithId([notification.id]);
      //   }
       
      // });
  
    }catch(e){
      console.log("ON_RECIEVED", JSON.stringify(e))
    }
    

  }
  whenAppCLosed(){
    
    // try{
      
    //   this.hmsPush.setBackgroundFile("public/assets/background.js")    
    //   .then((result) => console.log("setBackgroundFile", result))    
    //   .catch((error) => console.log("setBackgroundFile error", error));
    // }catch(e){
    //   console.log("BACKGROUND: ", JSON.stringify(e))
    // }
   
  }
  defaultSuccessHandler(tag: string, message: any) {
    message = message === undefined ? "" : message;
    console.log(tag, JSON.stringify(message));
  }

  defaultExceptionHandler(tag: string, err: any) {
    const message = "Error/Exception: " + JSON.stringify(err) + "\n";
    console.log(tag, JSON.stringify(message));
    // alert(`${tag} : ${message}`);
  }
  async turnOnPush() {
    this.hmsPush.turnOnPush()
      .then((result: any) => { this.defaultSuccessHandler("turnOnPush", result) })
      .catch((result: any) => { this.defaultExceptionHandler("turnOnPush", result) })
  }
  async getToken() {
    // const storedUserMobile: any = localStorage.getItem('userMobile');
    // const userMobiile = JSON.parse(storedUserMobile);
    this.hmsPush.getToken()
      .then((token: any) => { 
        this.defaultSuccessHandler("getToken", token);



      const storedDeviceToken: any = localStorage.getItem('deviceToken');
      let deviceTokenChanged = false;
      if (storedDeviceToken && storedDeviceToken !== 'undefined') {
        const deviceToken = JSON.parse(storedDeviceToken);
        if (deviceToken !== token) {
          localStorage.setItem('deviceToken', JSON.stringify(token));
          deviceTokenChanged = true;
        }
      }
      else {
        localStorage.setItem('deviceToken', JSON.stringify(token));
        deviceTokenChanged = true;
      }

      localStorage.setItem('deviceTokenChanged', JSON.stringify(deviceTokenChanged));
      const storeUserMobileChanged: any = localStorage.getItem('userMobileChanged');

      let isUserMobileChanged = false;
      if (storeUserMobileChanged && storeUserMobileChanged !== 'undefined') {
        isUserMobileChanged = Boolean(JSON.parse(storeUserMobileChanged));

      }
      if (deviceTokenChanged || isUserMobileChanged) {
        const storedUserMobile: any = localStorage.getItem('userMobile');

        if (storedUserMobile && storedUserMobile !== 'undefined') {//this is always true in this part
          const userMobiile = JSON.parse(storedUserMobile);
          this.registerMobile = new RegisterMobile();
          this.registerMobile.mobileNumber = userMobiile;
          this.registerMobile.token = token.value;
          localStorage.setItem('userMobileChanged', JSON.stringify(false));
          localStorage.setItem('deviceTokenChanged', JSON.stringify(false));

          this.commonServices.store.dispatch(
            notificationsActions.registerMobile({
              register: this.registerMobile,
            })
          );
        }
      }
      
      
      })
      .catch((result: any) => { this.defaultExceptionHandler("getToken", result) })

  }
 
  ngOnInit() {
    console.log('9999999999999999999999');

   
  }
}
