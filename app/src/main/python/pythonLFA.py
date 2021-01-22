#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""@author: ZL
"""
# importing required packages

import cv2 # openCV -- contains useful functions for image analysis and shape detection
import numpy as np
#import skimage # essentially scikit-learn image processing functions
#import skimage.measure
#from pyzbar import pyzbar
import matplotlib.pyplot as plt #remove this and debug in the end
import math
from os.path import dirname, join
from PIL import Image
import base64
import io


############### parameter templates for different lfd########

template_qr={'lfd_x1_ratio': 0, #lfd region x1 according to qr code: lfd_x1=qr_x1+qr_w*lfd_x1_ratio
             'lfd_y1_ratio': 3, #lfd region y1 according to qr code: lfd_y1=qr_y1+qr_w*lfd_y1_ratio
             'lfd_w_ratio': 1.5, #lfd region width according to qr code: lfd_w=w*lfd_w_ratio
             'lfd_h_ratio': 2.5, #lfd region height according to qr code: lfd_h=y*lfd_h_ratio
             }


template_bg={'lfd_x1_ratio': 0.3, #lfd region x1 according to qr code: lfd_x1=qr_x1+qr_w*lfd_x1_ratio
             'lfd_y1_ratio': 0.28, #lfd region y1 according to qr code: lfd_y1=qr_y1+qr_w*lfd_y1_ratio
             'lfd_w_ratio': 0.55, #lfd region width according to qr code: lfd_w=w*lfd_w_ratio
             'lfd_h_ratio': 0.2, #lfd region height according to qr code: lfd_h=y*lfd_h_ratio
             }


template_bg_1={'lfd_x1_ratio': 0.3, #lfd region x1 according to qr code: lfd_x1=qr_x1+qr_w*lfd_x1_ratio
               'lfd_y1_ratio': 0.28, #lfd region y1 according to qr code: lfd_y1=qr_y1+qr_w*lfd_y1_ratio
               'lfd_w_ratio': 0.65, #lfd region width according to qr code: lfd_w=w*lfd_w_ratio
               'lfd_h_ratio': 0.3, #lfd region height according to qr code: lfd_h=y*lfd_h_ratio
               }
############### some useful functions ############
def bgr2rgb(image):

    if len(image.shape)==3: #color
        b,g,r = cv2.split(image)       # get b,g,r
        image = cv2.merge([r,g,b])     # switch it to rgb

    return image

def sort_contours(cnts, method="left-to-right"):
    # initialize the reverse flag and sort index
    reverse = False
    i = 0

    # handle if we need to sort in reverse
    if method == "right-to-left" or method == "bottom-to-top":
        reverse = True

    # handle if we are sorting against the y-coordinate rather than
    # the x-coordinate of the bounding box
    if method == "top-to-bottom" or method == "bottom-to-top":
        i = 1

    # construct the list of bounding boxes and sort them from top to
    # bottom
    bounding_boxes = [cv2.boundingRect(c) for c in cnts]
    (cnts, bounding_boxes) = zip(*sorted(zip(cnts, bounding_boxes),
                                         key=lambda b: b[1][i], reverse=reverse))

    # return the list of sorted contours and bounding boxes
    return cnts, bounding_boxes

def inverse_colors(img):
    img = (255 - img)
    return img

#https://www.pyimagesearch.com/2014/08/25/4-point-opencv-getperspective-transform-example/
def order_points(pts):
    # initialzie a list of coordinates that will be ordered
    # such that the first entry in the list is the top-left,
    # the second entry is the top-right, the third is the
    # bottom-right, and the fourth is the bottom-left
    rect = np.zeros((4, 2), dtype = "float32")
    # the top-left point will have the smallest sum, whereas
    # the bottom-right point will have the largest sum
    s = pts.sum(axis = 1)
    rect[0] = pts[np.argmin(s)]
    rect[2] = pts[np.argmax(s)]
    # now, compute the difference between the points, the
    # top-right point will have the smallest difference,
    # whereas the bottom-left will have the largest difference
    diff = np.diff(pts, axis = 1)
    rect[1] = pts[np.argmin(diff)]
    rect[3] = pts[np.argmax(diff)]
    # return the ordered coordinates
    return rect


def four_point_transform(image, pts):
    # obtain a consistent order of the points and unpack them
    # individually
    #rect = order_points(pts)
    #(tl, tr, br, bl) = rect
    rect=pts
    (tl, tr, br, bl) = rect
    # compute the width of the new image, which will be the
    # maximum distance between bottom-right and bottom-left
    # x-coordiates or the top-right and top-left x-coordinates
    widthA = np.sqrt(((br[0] - bl[0]) ** 2) + ((br[1] - bl[1]) ** 2))
    widthB = np.sqrt(((tr[0] - tl[0]) ** 2) + ((tr[1] - tl[1]) ** 2))
    maxWidth = max(int(widthA), int(widthB))
    # compute the height of the new image, which will be the
    # maximum distance between the top-right and bottom-right
    # y-coordinates or the top-left and bottom-left y-coordinates
    heightA = np.sqrt(((tr[0] - br[0]) ** 2) + ((tr[1] - br[1]) ** 2))
    heightB = np.sqrt(((tl[0] - bl[0]) ** 2) + ((tl[1] - bl[1]) ** 2))
    maxHeight = max(int(heightA), int(heightB))
    # now that we have the dimensions of the new image, construct
    # the set of destination points to obtain a "birds eye view",
    # (i.e. top-down view) of the image, again specifying points
    # in the top-left, top-right, bottom-right, and bottom-left
    # order
    dst = np.array([
        [0, 0],
        [maxWidth - 1, 0],
        [maxWidth - 1, maxHeight - 1],
        [0, maxHeight - 1]], dtype = "float32")
    # compute the perspective transform matrix and then apply it
    M = cv2.getPerspectiveTransform(rect, dst)
    #warped = cv2.warpPerspective(image, M, (maxWidth, maxHeight))
    rows,cols = image.shape[:2]
    warped = cv2.warpPerspective(image, M,(cols,rows))
    # return the warped image
    return warped,[maxWidth, maxHeight]

def cal_box_ratio(box):

    #the list is the top-left,
    # the second entry is the top-right, the third is the
    # bottom-right, and the fourth is the bottom-left
    rect = order_points(box)
    (tl, tr, br, bl) = rect
    # compute the width of the new image, which will be the
    # maximum distance between bottom-right and bottom-left
    # x-coordiates or the top-right and top-left x-coordinates
    widthA = np.sqrt(((br[0] - bl[0]) ** 2) + ((br[1] - bl[1]) ** 2))
    widthB = np.sqrt(((tr[0] - tl[0]) ** 2) + ((tr[1] - tl[1]) ** 2))
    maxWidth = max(int(widthA), int(widthB),1)
    # compute the height of the new image, which will be the
    # maximum distance between the top-right and bottom-right
    # y-coordinates or the top-left and bottom-left y-coordinates
    heightA = np.sqrt(((tr[0] - br[0]) ** 2) + ((tr[1] - br[1]) ** 2))
    heightB = np.sqrt(((tl[0] - bl[0]) ** 2) + ((tl[1] - bl[1]) ** 2))
    maxHeight = max(int(heightA), int(heightB),1)

    if maxHeight>maxWidth:
        ratio=maxHeight/maxWidth
    else:
        ratio=maxWidth/maxHeight

    area=maxWidth*maxHeight
    return ratio,area

def connected_component_analysis(img):
    # perform a connected component analysis on the thresholded image
    # essentially, this helps identify the "largest blobs"
    # https://www.pyimagesearch.com/2016/10/31/detecting-multiple-bright-spots-in-an-image-with-python-and-opencv/

    #labels = skimage.measure.label(image_no_noise, neighbors=8, background=0)
    #mask = np.zeros(image_no_noise.shape, dtype="uint8")

    # loop over the unique components
    #for label in np.unique(labels):
    # if this is the background label, ignore it
    #    if label == 0:
    #        continue

    # otherwise, construct the label mask and count the
    # number of pixels
    #    labelMask = np.zeros(image_no_noise.shape, dtype="uint8")
    #    labelMask[labels == label] = 255
    #    numPixels = cv2.countNonZero(labelMask)

    # if the number of pixels in the component is sufficiently
    # large, then add it to our mask of "large blobs"
    # only add to mask if the identified sample and/or control band is sufficiently large
    # sufficiently large here means that the band contains at the very least 3*image_width number of pixels
    #    if numPixels > mask.shape[1]*5: # new threshold as of 1/7/2020
    # if numPixels > 300:
    #        mask = cv2.add(mask, labelMask)

    #return mask
    pass

def in_box(small,large):
    #[x,y,w,h]
    if small[0]>large[0] and (small[0]+small[2]) <(large[0]+large[2]):

        if small[1]>large[1] and (small[1]+small[3]) <(large[1]+large[3]):

            return 1

    else:
        return 0


def auto_canny(image, sigma=0.33):
    # compute the median of the single channel pixel intensities
    v = np.median(image)
    # apply automatic Canny edge detection using the computed median
    lower = int(max(0, (1.0 - sigma) * v))
    upper = int(min(255, (1.0 + sigma) * v))
    edged = cv2.Canny(image, lower, upper)
    # return the edged image
    return edged


def sort_by_tl_point(box_org,tl_point):

    min_dis=0
    min_point=0
    #find the nearest point from boc_org
    for i,point in enumerate(box_org):

        dis = np.sqrt(((point[0] - tl_point[0]) ** 2) + ((point[1] - tl_point[1]) ** 2))

        if i==0:
            min_dis=dis
        else:
            if dis<	min_dis:
                min_dis=dis
                min_point=i

    new_box=[]

    #sort
    for i,point in enumerate(box_org):
        #print('min_point',min_point)
        new_box.append(box_org[min_point])

        min_point=min_point+1
        if min_point>3:
            min_point=0

    return new_box



#############################

class LateralFlowAnalysis():

    def __init__(self):
        pass

    def qr_detection(self,img,tmeplate):


        lfd_images_list=[]
        ref_area_list=[]

        #img = cv2.medianBlur(img,3)

        #Scaling
        scale=0
        barcodes = []
        height, width = img.shape[:2]

        #Scale increasing for qr search
        while scale<12 and len(barcodes)==0 and scale*width<8000:

            scale=scale+1
            img_scale = cv2.resize(img,(scale*width, scale*height), interpolation = cv2.INTER_CUBIC)
            # find the barcodes in the image and decode each of the barcodes
            barcodes = pyzbar.decode(img_scale)

        self.msg_debug_list.append("resize QR image {}/{}\n".format(scale,img_scale.shape))


        #increase scale to check if it can find any new qr
        for i in range(3):
            new_scale=scale+i+1
            img_scale = cv2.resize(img,(new_scale*width, new_scale*height), interpolation = cv2.INTER_CUBIC)
            # find the barcodes in the image and decode each of the barcodes
            barcodes = pyzbar.decode(img_scale)

        if len(barcodes)>=1: #if find more ,use new scale
            scale=new_scale

        else: #otherwise keep the old one

            img_scale = cv2.resize(img,(scale*width, scale*height), interpolation = cv2.INTER_CUBIC)
            # find the barcodes in the image and decode each of the barcodes
            barcodes = pyzbar.decode(img_scale)

        #(x, y, w, h) = barcodes[0].rect
        #print(w,h)
        #print(len(barcodes),barcodes)

        # loop over the detected barcodes
        for barcode in barcodes:
            # extract the bounding box location of the barcode and draw the
            # bounding box surrounding the barcode on the image
            #(x, y, w, h) = barcode.rect
            #cv2.rectangle(img, (x, y), (x + w, y + h), (0, 0, 255), 5)
            # the barcode data is a bytes object so if we want to draw it on
            # our output image we need to convert it to a string first
            barcodeData = barcode.data.decode("utf-8")
            #barcodeType = barcode.type
            # draw the barcode data and barcode type on the image
            #self.text = "{} ({})".format(barcodeData, barcodeType)
            self.text = "{}".format(barcodeData)
            #cv2.putText(img, self.text, (x, y - 10), cv2.FONT_HERSHEY_SIMPLEX,3, (0, 0, 255), 2)


            point_list=[]
            for points in barcode.polygon:

                point_list.append([points.x,points.y])

                #print(point_list)

                pts = np.array((point_list), dtype = "float32")
                warped,qr_size = four_point_transform(img_scale, pts)

            #change image back
            warped = cv2.resize(warped,(width,height), interpolation = cv2.INTER_CUBIC)
            qr_size[0]=int(qr_size[0]/scale)
            qr_size[1]=int(qr_size[1]/scale)
            #####

            cv2.rectangle(warped, (0, 0), (qr_size[0], qr_size[1]), (0, 0, 255), 5)

            self.img_debug_list.append(['QRX'+str(scale),warped])

            lfd_image=self.lfd_location(warped.copy(),[0,0,qr_size[0], qr_size[1]],tmeplate)
            lfd_images_list.append(lfd_image)
            ref_area=qr_size[0]*qr_size[1]
            ref_area_list.append(ref_area)


        return lfd_images_list,ref_area_list


    def background_extraction(self,img,tmeplate):

        self.text="unknown"
        #need to find a better reference area
        height, width = img.shape[:2]
        img_area= height*width

        img_gray = cv2.cvtColor(img.copy(), cv2.COLOR_BGR2GRAY)
        img_blurred = cv2.GaussianBlur(img_gray, (3, 3), 0)


        th, im_th = cv2.threshold(img_blurred, 100, 255, cv2.THRESH_BINARY_INV);


        img_floodfill = im_th.copy()

        h, w = im_th.shape[:2]

        mask = np.zeros((h+2, w+2), np.uint8)

        cv2.floodFill(img_floodfill, mask, (0,0), 255);

        img_floodfill_inv = cv2.bitwise_not(img_floodfill)

        #img_foreground = im_th | img_floodfill_inv

        img_foreground = img_floodfill_inv
        #img_foreground = cv2.erode(img_foreground,kernel,iterations = 1)
        #img_foreground = cv2.dilate(img_foreground,kernel,iterations = 2)

        #self.img_debug_list.append(['im_th ',im_th ])
        #self.img_debug_list.append(['im_floodfill_inv',img_floodfill_inv])
        #self.img_debug_list.append(['img_foreground',img_foreground])

        #####find contours
        _, contours, heirarchy = cv2.findContours(img_foreground,cv2.RETR_TREE,cv2.CHAIN_APPROX_SIMPLE)

        #print(len(contours))

        lfd_images_list = []
        ref_area_list=[]

        #print('contour',len(contours))
        for i,c in enumerate(contours):

            #if heirarchy[0][i][3]==-1:
            rect = cv2.minAreaRect(c)
            box = cv2.boxPoints(rect)
            box = np.int0(box)
            box_ratio,area=cal_box_ratio(box)
            #print('box_ratio',ratio)

            if box_ratio>1 and box_ratio<8 and area>img_area*0.01: #check this again later
                #img = cv2.drawContours(img,[box],0,(0,0,255),5)

                pts = np.array((box), dtype = "float32")
                warped,box_size = four_point_transform(img, pts)

                img_debug=warped.copy()
                cv2.rectangle(img_debug, (0, 0), (box_size[0], box_size[1]), (0, 0, 255), 3)
                self.img_debug_list.append(['extraction',img_debug])

                lfd_image=self.lfd_location(warped.copy(),[0,0,box_size[0], box_size[1]],tmeplate)
                lfd_images_list.append(lfd_image)

                ref_area=area/20 #keep it is similar bf/(2*7.5) to qr code
                ref_area_list.append(ref_area)

        return lfd_images_list,ref_area_list


    def initialzie_feature_match(self,template_img):

        FLANN_INDEX_KDTREE = 0
        index_params = dict(algorithm = FLANN_INDEX_KDTREE, trees = 5)
        search_params = dict(checks = 50)
        flann = cv2.FlannBasedMatcher(index_params, search_params)

        template=cv2.imread(template_img)
        template= cv2.cvtColor(template,cv2.COLOR_RGB2GRAY)
        surf = cv2.xfeatures2d_SURF.create(hessianThreshold=400)
        kp1, des1 = surf.detectAndCompute(template,None)

        return kp1,des1,flann

    def lfd_feature_match(self,img,kp1,des1,flann):


        img2=img.copy()
        img2 = cv2.cvtColor(img2,cv2.COLOR_RGB2GRAY)

        surf = cv2.xfeatures2d_SURF.create(hessianThreshold=400)
        kp2, des2 = surf.detectAndCompute(img2,None)

        # store all the good matches as per Lowe's ratio test.

        matches = flann.knnMatch(des1,des2,k=2)
        good = []

        for m,n in matches:
            if m.distance < 0.7*n.distance:
                good.append(m)

        return [len(good),good,kp1,kp2]


    def lfd_image_transform(self,img,good,kp1,kp2,template_img,template_bg):


        img_org=img.copy()
        img2=img.copy()
        height, width = img.shape[:2]
        img_area= height*width


        template=cv2.imread(template_img)
        template= cv2.cvtColor(template,cv2.COLOR_RGB2GRAY)


        lfd_images_list=[]
        ref_area_list=[]


        MIN_MATCH_COUNT=10

        if len(good)>MIN_MATCH_COUNT:
            src_pts = np.float32([ kp1[m.queryIdx].pt for m in good ]).reshape(-1,1,2)
            dst_pts = np.float32([ kp2[m.trainIdx].pt for m in good ]).reshape(-1,1,2)

            M, mask = cv2.findHomography(src_pts, dst_pts, cv2.RANSAC,5.0)
            matchesMask = mask.ravel().tolist()

            h, w = template.shape[:2]
            pts = np.float32([ [0,0],[0,h-1],[w-1,h-1],[w-1,0] ]).reshape(-1,1,2)
            dst = cv2.perspectiveTransform(pts,M)

            img2 = cv2.polylines(img2,[np.int32(dst)],True,255,3, cv2.LINE_AA)
            img_poly=cv2.polylines(img_org.copy(),[np.int32(dst)],True,255,30, cv2.LINE_AA)


            tl_point=np.int32(dst)[0][0]
            img_poly = cv2.circle(img_poly, (tl_point[0],tl_point[1]), radius=100, color=(0, 0, 255), thickness=-1)
            #self.img_debug_list.append(['match_poly',img_poly])

            rect = cv2.minAreaRect(np.int32(dst))
            box_org = cv2.boxPoints(rect)
            box_org = np.int0(box_org)

            box_ratio,area=cal_box_ratio(box_org)
            #img_rect=cv2.rectangle(img_org.copy(), (int(rect[0]),int(rect[1])), (int(rect[0]+rect[2]), int(rect[1]+rect[3])), (0, 0, 255), 6)
            img_rect=cv2.drawContours(img_org.copy(),[box_org],0,(0,0,255),6)

            box=sort_by_tl_point(box_org,tl_point)


            for i in range(len(box)):
                img_rect = cv2.circle(img_rect, (box[i][0],box[i][1]), radius=1, color=(0, 0, 255), thickness=-1)
                cv2.putText(img_rect,str(i),(box[i][0],box[i][1]),cv2.FONT_HERSHEY_SIMPLEX,3,(0,0,255),10,cv2.LINE_AA)

            self.img_debug_list.append(['match_rect',img_rect])

            #print("Good matches are found - %d/%d" % (len(good),MIN_MATCH_COUNT))


            if box_ratio>1 and box_ratio<8 and area>img_area*0.01: #check this again later
                #img = cv2.drawContours(img,[box],0,(0,0,255),5)

                pts = np.array((box), dtype = "float32")
                warped,box_size = four_point_transform(img, pts)

                img_debug=warped.copy()

                cv2.rectangle(img_org, (0, 0), (box_size[0], box_size[1]), (0, 0, 255), 6)
                self.img_debug_list.append(['extraction',img_debug])

                #check if it needs to rotate 90 degrees
                height, width = warped.shape[:2]
                #print(height, width)
                #need to add 90 or 270

                lfd_image=self.lfd_location(warped.copy(),[0,0,box_size[0], box_size[1]],template_bg)

                lfd_images_list.append(lfd_image)

                ref_area=area/20 #keep it is similar bf/(2*7.5) to qr code
                ref_area_list.append(ref_area)


        else:

            print("Not enough matches are found - %d/%d" % (len(good),MIN_MATCH_COUNT))
            matchesMask = None


            #draw_params = dict(matchColor = (0,0,255),singlePointColor = None,matchesMask = matchesMask, flags = 2)
            #img3 = cv2.drawMatches(img1,kp1,img2,kp2,good,None,**draw_params)

        #img2=cv2.cvtColor(img2,cv2.COLOR_GRAY2BGR)
        #self.img_debug_list.append(['match',img_org])

        return lfd_images_list,ref_area_list



    def lfd_location(self,img,pos,template):


        x1=int(pos[0]+pos[2]*template['lfd_x1_ratio'])
        y1=int(pos[1]+pos[3]*template['lfd_y1_ratio'])
        w=int(pos[2]*template['lfd_w_ratio'])
        h=int(pos[3]*template['lfd_h_ratio'])

        img_org=img.copy()
        #img = cv2.rectangle(img,(x1,y1),(x1+w,y1+h),(255,0,0),6)
        #self.img_debug_list.append(['location',img])

        lfd_image=img_org[y1:y1+h,x1:x1+w]
        self.img_debug_list.append(['roi',lfd_image])

        return lfd_image


    def image_processing(self,img,ref_area):

        image_RBG = img.copy()
        image_Test = img.copy()

        image_GRAY = cv2.cvtColor(image_RBG, cv2.COLOR_RGB2GRAY)
        #image_GRAY =cv2.normalize(image_GRAY,image_GRAY ,0,255,cv2.NORM_MINMAX)
        # invert image colors -- again, following what Prof. Kaminski does
        # during signal quantification
        #image_invert = cv2.bitwise_not(image_GRAY)
        # saving original inverted image such that contour box is not saved over
        #image_invert_orig = image_invert.copy()

        #image_blur = cv2.GaussianBlur(image_invert, (5, 5), 0)
        image_blur = image_GRAY

        #
        img_hsv = cv2.cvtColor(img.copy(), cv2.COLOR_BGR2HSV)
        #lower = np.array([0,50,50])
        #upper = np.array([255,255,255])
        #img_hsv = cv2.inRange(img_hsv, lower, upper)
        #image_GRAY = cv2.cvtColor(image_RBG, cv2.COLOR_RGB2GRAY)
        #self.img_debug_list.append(['image_hsv',img_hsv])

        #inverse here targe is white color

        image_blur=inverse_colors(image_blur.copy())
        #thresh
        #ret2,image_thresh = cv2.threshold(image_blur,0,255,cv2.THRESH_BINARY+cv2.THRESH_OTSU)
        #image_thresh = cv2.adaptiveThreshold(image_blur,255,cv2.ADAPTIVE_THRESH_MEAN_C,cv2.THRESH_BINARY,127, 11)


        C_region=[]
        T_region=[]

        #thresh = float(cv2.meanStdDev(image_blur)[0]) + 0.7*float(cv2.meanStdDev(image_blur)[1])

        thresh=150
        while len(C_region)!=1 and len(T_region)!=1:

            thresh=thresh-10

            if thresh <50:
                return false


            #self.msg_debug_list.append("binarize thresh:{}\n".format(thresh))
            image_thresh = cv2.threshold(image_blur.copy(), thresh, 255, cv2.THRESH_BINARY)[1]
            #self.img_debug_list.append(['image_thresh',image_thresh])

            kernel = np.ones((1,1),np.uint8) #for while color
            image_erode = cv2.erode(image_thresh, kernel, iterations=1)
            #kernel = np.ones((2,2),np.uint8) #for while color
            image_no_noise = cv2.dilate(image_erode, kernel, iterations=1)
            #self.img_debug_list.append(['img_process',image_no_noise])
            #########

            # find the contours in the mask, then sort them from left-to-right
            _,contours,hierarchy = cv2.findContours(image_no_noise.copy(), cv2.RETR_EXTERNAL,cv2.CHAIN_APPROX_SIMPLE)
            #self.msg_debug_list.append("find contours:{}\n".format(len(contours)))

            if len(contours) > 0:
                C_region,T_region,test_line_candidates=self.TC_detection(contours,image_no_noise.copy(),ref_area)

            #print('thresh',thresh)
            #print('C_r',len(C_region))
            #print('T_r',len(T_region))


        self.img_debug_list.append(['image_no_noise',image_no_noise])
        return C_region,T_region,test_line_candidates


    def TC_detection(self,contours,img,ref_area):

        C_region=[]
        T_region=[]
        test_line_candidates=[]

        contours, _ = sort_contours(contours)
        for contour in contours:

            area = cv2.contourArea(contour)
            if area > ref_area*0.005 and area < ref_area*0.1:  # refer to qr code area

                #search "T","C", and test_line_candidate
                #cv2.drawContours(img, [contour], 0, (0,0,255), 1)
                [x, y, w, h] = cv2.boundingRect(contour)

                top_l, top_r, bot_l, bot_r = int(x), int(x+w), int(y), int(y+h)
                cv2.rectangle(img,(top_l,bot_l),(top_r,bot_r),(0,255,0),1)
                roi=img[y:y + h, x: x + w]


                # search for "T" and "C"
                if self.find_TC(roi,'C') ==1:
                    #self.img_debug_list.append(['C',roi])
                    C_region.append([x,y,w,h])


                elif self.find_TC(roi,'T') ==1:
                    #self.img_debug_list.append(['T',roi])
                    T_region.append([x,y,w,h])

                else:
                    test_line_candidates.append([x,y,w,h])


        #self.img_debug_list.append(['contour',img])

        return C_region,T_region,test_line_candidates


    def find_TC(self,img,character):

        #Scaling
        height, width = img.shape[:2]
        #print(height, width)
        times=int(40/width)
        if times==0:
            times=3

        img = cv2.resize(img,(times*width, times*height), interpolation = cv2.INTER_CUBIC)
        #dHC = int(roiH * 0.05)
        (x, y, w, h) = cv2.boundingRect(img)
        (roiH, roiW) = img.shape
        (dW, dH) = (int(roiW * 0.25), int(roiH * 0.3))

        segments_C = [
            ((dW, 0), (w, dH)),  # top
            ((0, dH), (dW, h-dH)), # left
            ((dW, h - dH), (w, h)),   # bottom
            ((dW,dH),(w,h-dH)) #middle
        ]

        segments_T = [
            ((0, 0), (w, dH)),  # top
            ((0, dH), (dW, h )), # left
            ((dW, dH), (w-dW, h)), # middle
            ((w-dW, dH), (w, h)),   # right
        ]

        if character=='C':
            segments=segments_C

        if character=='T':
            segments=segments_T

        on = [0] * len(segments)

        # loop over the segments
        img_color=cv2.cvtColor(img,cv2.COLOR_GRAY2BGR)
        for (i, ((xA, yA), (xB, yB))) in enumerate(segments):

            segROI = img[yA:yB, xA:xB]
            cv2.rectangle(img_color,(xA, yA), (xB, yB),(0,255,0),1)
            total = cv2.countNonZero(segROI)
            area = (xB - xA) * (yB - yA)
            #print(total / float(area))
            if (total / float(area)) > 0.7:
                on[i]= 1


        #self.img_debug_list.append(['rois',img_color])

        if character=='C':
            template=[1,1,1,0]
            if template==on:
                self.msg_debug_list.append("find C\n")
                return 1

            else:
                return 0

        if character=='T':
            template=[1,0,1,0]
            if template==on:
                self.msg_debug_list.append("find T\n")
                return 1

            else:
                return 0

        return on


    def local_image_process(self,img):
        #print('shape')
        #print('shape',img.shape)

        height, width = img.shape[:2]
        area = height*width

        #image_GRAY = cv2.cvtColor(img, cv2.COLOR_RGB2GRAY)
        #image_blur = cv2.GaussianBlur(image_GRAY, (11, 11), 0)


        #thresh = float(cv2.meanStdDev(image_blur)[0]) + 0.9*float(cv2.meanStdDev(image_blur)[1]) # new threshold as of 1/7/2020
        #thresh = float(cv2.meanStdDev(image_blur)[0]) -2*float(cv2.meanStdDev(image_blur)[1])
        #image_thresh = cv2.threshold(image_blur, thresh, 255, cv2.THRESH_BINARY)[1]
        #ret2,image_thresh = cv2.threshold(image_GRAY,0,255,cv2.THRESH_BINARY+cv2.THRESH_OTSU)
        #image_thresh = cv2.adaptiveThreshold(image_blur ,255,cv2.ADAPTIVE_THRESH_MEAN_C,cv2.THRESH_BINARY,127, 11)
        image_thresh =inverse_colors(img.copy())

        ratio = cv2.countNonZero(image_thresh)/ float(area)
        self.img_debug_list.append(['local_TC'+str(ratio),image_thresh])

        return ratio

        #_, contours, heirarchy = cv2.findContours(im_th,cv2.RETR_TREE,cv2.CHAIN_APPROX_SIMPLE)


    def create_TC_test_regions(self,region):

        x1=int(region[0]-region[2]*3.5)#shapes_pos[0][0] #the right boundary
        y1=int(region[1])
        x2=int(region[0]-region[2]*2.5)
        y2=int(region[1]+region[3]*1.7)
        test_region=[x1,y1,x2-x1,y2-y1]

        #cv2.rectangle(image_RBG,(x1,y1),(x2,y2),(0,255,0),1)
        return test_region



    def estimate_T_test_regions(self,region):

        x1=int(region[0]-region[2]*3.5)#shapes_pos[0][0] #the right boundary
        y1=int(region[1]+region[3]*2)
        x2=int(region[0]-region[2]*2.5)
        y2=int(region[1]+region[3]*3.5)
        test_region=[x1,y1,x2-x1,y2-y1]

        #cv2.rectangle(image_RBG,(x1,y1),(x2,y2),(0,255,0),1)
        return test_region

    def estimate_C_test_regions(self,region):

        x1=int(region[0]-region[2]*3.5)#shapes_pos[0][0] #the right boundary
        y1=int(region[1]-region[3]*3)
        x2=int(region[0]-region[2]*2.5)
        y2=int(region[1]-region[3]*0.2)
        test_region=[x1,y1,x2-x1,y2-y1]

        #cv2.rectangle(image_RBG,(x1,y1),(x2,y2),(0,255,0),1)
        return test_region

    def lfd_analysis(self,img,C_region_list,T_region_list,test_line_candidates):

        image_RBG=img.copy()
        image_Test=img.copy()
        results='none'

        # check image processing results: C_region and T_region


        if len(C_region_list)==1 and len(T_region_list)==1:

            self.msg_debug_list.append("T and C regions detected\n")
            T_test_region=self.create_TC_test_regions(T_region_list[0])
            C_test_region=self.create_TC_test_regions(C_region_list[0])

        elif len(C_region_list)==1 and len(T_region_list)!=1:

            self.msg_debug_list.append("C region detected\n")
            C_test_region=self.create_TC_test_regions(C_region_list[0])
            T_test_region=self.estimate_T_test_regions(C_region_list[0])

        elif len(C_region_list)!=1 and len(T_region_list)==1:
            self.msg_debug_list.append("T region detected\n")
            T_test_region=self.create_TC_test_regions(T_region_list[0])
            C_test_region=self.estimate_C_test_regions(T_region_list[0])

        else:
            self.msg_debug_list.append("no region detected\n")
            return


        #print('C_test_region',C_test_region)


        image_GRAY = cv2.cvtColor(img.copy(), cv2.COLOR_RGB2GRAY)
        image_blur = cv2.GaussianBlur(image_GRAY, (11, 11), 0)

        image_thresh = cv2.adaptiveThreshold(image_blur ,255,cv2.ADAPTIVE_THRESH_MEAN_C,cv2.THRESH_BINARY,127, 11)
        #thresh = float(cv2.meanStdDev(image_blur)[0]) + 0.9*float(cv2.meanStdDev(image_blur)[1]) # new threshold as of 1/7/2020
        #thresh = float(cv2.meanStdDev(image_blur)[0]) -2*float(cv2.meanStdDev(image_blur)[1])
        #image_thresh = cv2.threshold(image_blur, thresh, 255, cv2.THRESH_BINARY)[1]

        img_C=image_thresh[C_test_region[1]:C_test_region[1]+C_test_region[3],C_test_region[0]:C_test_region[0]+C_test_region[2]]
        img_T=image_thresh[T_test_region[1]:T_test_region[1]+T_test_region[3],T_test_region[0]:T_test_region[0]+T_test_region[2]]
        value_C=self.local_image_process(img_C.copy())
        value_T=self.local_image_process(img_T.copy())

        cv2.rectangle(image_RBG,(T_test_region[0],T_test_region[1]),(T_test_region[0]+T_test_region[2],T_test_region[1]+T_test_region[3]),(0,0,255),3)
        cv2.rectangle(image_RBG,(C_test_region[0],C_test_region[1]),(C_test_region[0]+C_test_region[2],C_test_region[1]+C_test_region[3]),(0,255,0),3)
        #self.img_debug_list.append(['roi',image_RBG.copy()])


        control_line=[]
        test_line=[]
        #####################method1
        if value_C==0:
            value_C=0.001

        if value_T==0:
            value_T=0.001


        ratio_CT=value_C/value_T



        #print('ratio_CT',ratio_CT,value_C,value_T)
        if value_C>0.05 and value_T>0.05 and ratio_CT>0.5:
            control_line.append([0,0,1,1])
            test_line.append([0,0,1,1])

        elif value_C>0.05 and ratio_CT>=0.5:
            control_line.append([0,0,1,1])

        elif value_T>0.05 and ratio_CT<=0.5:
            test_line.append([0,0,1,1])
        else:
            pass


        ###############################method2

        # check test line
        #for line in test_line_candidates:

        #    if in_box(line,C_test_region):
        #        control_line.append(line)
        #        cv2.rectangle(image_RBG,(line[0],line[1]),(line[0]+line[2],line[1]+line[3]),(0,255,0),1)
        #img = cv2.putText(img,"Positive",(0,0),cv2.FONT_HERSHEY_SIMPLEX,5,(0,0,255),3,cv2.LINE_AA)


        #    if in_box(line,T_test_region):
        #        test_line.append(line)
        #        cv2.rectangle(image_RBG,(line[0],line[1]),(line[0]+line[2],line[1]+line[3]),(0,0,255),1)
        #img = cv2.putText(img,"Positive",(0,0),cv2.FONT_HERSHEY_SIMPLEX,5,(0,0,255),3,cv2.LINE_AA)

        self.img_debug_list.append(['result',image_RBG])

        image_RBG= cv2.resize(image_RBG,(720,1024), interpolation = cv2.INTER_CUBIC)

        #pos1=T_test_region[0]+T_test_region[2]
        #pos2=T_test_region[1]+T_test_region[3]

        pos1=0
        pos2=200
        if len(control_line)==1 and len(test_line)==1:
            cv2.putText(image_RBG,"Positive",(pos1,pos2),cv2.FONT_HERSHEY_SIMPLEX,3,(0,0,255),5,cv2.LINE_AA)
            results=self.text+','+"Positive"

        elif len(control_line)==1 and len(test_line)==0:
            cv2.putText(image_RBG,"Negative",(pos1,pos2),cv2.FONT_HERSHEY_SIMPLEX,3,(0,0,255),5,cv2.LINE_AA)
            results=self.text+','+"Negative"

        elif len(control_line)==0 and len(test_line)==0:
            cv2.putText(image_RBG,"Invalid",(pos1,pos2),cv2.FONT_HERSHEY_SIMPLEX,3,(0,0,255),5,cv2.LINE_AA)
            results=self.text+','+"Invalid"
        else:
            cv2.putText(image_RBG,"Fail to process",(pos1,pos2),cv2.FONT_HERSHEY_SIMPLEX,3,(0,0,255),5,cv2.LINE_AA)
            results="Fail"

        #cv2.putText(image_RBG, self.text, (0, 100), cv2.FONT_HERSHEY_SIMPLEX,3, (0, 0, 255), 5)

        self.img_debug_list.append(['results',image_RBG])

        return results


    def img_debug(self):

        plt.close("all")
        fig=plt.figure(figsize=(8, 8))
        col=math.ceil(len(self.img_debug_list)/3)

        for i,img in enumerate(self.img_debug_list):

            fig.add_subplot(3, col, i+1)

            plt.title(img[0])
            #if len(img[1].shape)==2: #grey
            plt.imshow(bgr2rgb(img[1]),'gray')

            #cv2.imwrite(name, img[1])
            plt.xticks([])
            plt.yticks([])

        plt.show()

    def bundle_img_debug(self):

        bundle_debug_list=[] #less images then img_debug_list[]
        #bundle_name_list=['org','match_rect',extraction','roi','image_no_noise','results']
        bundle_name_list=['results']

        for img in self.img_debug_list:

            if img[0] in bundle_name_list:
                bundle_debug_list.append([img[0],img[1]])


        return bundle_debug_list

    def msg_debug(self):
        msgs=''
        for msg in self.msg_debug_list:
            msgs=msgs+msg
        return msgs



    def run(self,img,template=template_qr):

        self.img_debug_list=[]
        self.msg_debug_list=[]
        self.text=""

        #print(img_name)
        #img = cv2.imread(img_name) #load image

        #img = cv2.resize(img,(scale*width, scale*height), interpolation = cv2.INTER_CUBIC)
        results='Fail'

        if img is None:

            results=results+':'+"image empty"
            return results
        else:
            height, width = img.shape[:2]

            self.msg_debug_list.append("load image successful! {}X{}\n".format(height,width))
            self.img_debug_list.append(['org',img])


        lfd_images_list=[]


        #qr detetion and lfd location
        #try:
        #    template=template_qr
        #    lfd_images_list,ref_area_list=self.qr_detection(img.copy(),template_qr)
        #except:
        #    self.msg_debug_list.append("not use QR and use background extraction\n")
        #    lfd_images_list,ref_area_list=self.background_extraction(img.copy(),template_bg)


        if len(lfd_images_list)==0:
            #try:
            #self.msg_debug_list.append("not detect QR and use background extraction\n")
            #lfd_images_list,ref_area_list=self.background_extraction(img.copy(),template_bg_1)
            #except:
            #    self.msg_debug_list.append("something wrong in background extraction\n")
            try:
                match_result=[]
                count=0
                while count<2:
                    filename1 = join(dirname(__file__), 'target'+str(count)+'.png')
                    kp1,des1,flann=self.initialzie_feature_match(filename1)
                    match_result.append(self.lfd_feature_match(img,kp1,des1,flann))
                    #print('match_result',match_result[count][0])
                    count=count+1


                max_match=0
                max_index=0
                for i,match in enumerate(match_result):
                    #print(match_result[i][0] )
                    if match_result[i][0] >  max_match:
                        max_match=match_result[i][0]
                        max_index=i

                        #print(max_index,max_match)
                good=match_result[max_index][1]
                kp1=match_result[max_index][2]
                kp2=match_result[max_index][3]
                filename2 = join(dirname(__file__), 'target'+str(max_index)+'.png')

                print('PYTHON test1')
                lfd_images_list,ref_area_list=self.lfd_image_transform(img,good,kp1,kp2,filename2,template_bg_1)
                print('PYTHON test2')
            except:
                print('error')


        if len(lfd_images_list)>0:

            self.msg_debug_list.append("find lfd region X{}\n".format(len(lfd_images_list)))

            for i,lfd_image in enumerate(lfd_images_list):
                try:
                    C_region,T_region,test_line_candidates=self.image_processing(lfd_image.copy(),ref_area_list[i])
                    results=self.lfd_analysis(lfd_image.copy(),C_region,T_region,test_line_candidates)
                except:
                    self.msg_debug_list.append("something wrong in image_processing\n")

                    #self.img_debug()

        bundle_debug_list=self.bundle_img_debug()

        debug_msg=self.msg_debug()
        results=results+":\n"+debug_msg

        return results, bundle_debug_list



def runLFA(kotlinByteArrayStr):

    decoded_data = base64.b64decode(kotlinByteArrayStr)
    np_data = np.fromstring(decoded_data,np.uint8)
    img = cv2.imdecode(np_data,cv2.IMREAD_UNCHANGED)

    lfa= LateralFlowAnalysis()
    results,bundle_debug_list=lfa.run(img,template_qr)

    return results
