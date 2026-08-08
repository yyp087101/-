\
\
\
\
   
import math


def _r2(v):
    return round(float(v), 2)


def _round_pts(pts):
    return [(_r2(x), _r2(y)) for x, y in pts]


NVXIKU_BASELINE_INPUT = {"W_s": 68.0, "H_s": 90.0, "yao_gao": 98.0, "zuo_gao": 25.0}
NVXIKU_BASELINE_FRONT = [
    (10.00, 0.00), (11.50, 44.50), (13.65, 71.00), (14.35, 79.00), (12.35, 95.00),
    (-8.65, 95.00), (-9.65, 79.00), (-11.78, 72.07), (-13.65, 71.00), (-11.50, 44.50), (-10.00, 0.00),
]
NVXIKU_BASELINE_BACK = [
    (12.00, 0.00), (13.50, 44.50), (19.00, 79.00), (19.09, 95.00), (-3.32, 96.99), (-3.50, 95.00),
    (-7.00, 79.00), (-10.97, 71.50), (-18.97, 70.00), (-16.22, 57.25), (-13.50, 44.50), (-12.00, 0.00),
]
NVXIKU_BASELINE_DART = [(5.18, 95.00), (7.18, 95.00), (6.18, 82.00)]

                                                              
                  
                                                              
def calc_nvxiku(W_s, H_s, yao_gao, zuo_gao):
                                   
    H = H_s + 2
    W = W_s + 2
    wb_w = 4
    Y_w = yao_gao + 1 - wb_w                   
    rise = zuo_gao + 3 - wb_w                 
                             
                                         
    Y_hip = Y_w - 0.64 * zuo_gao
    Y_mid = Y_hip / 2 + 5                   
    Y_cr = Y_mid
    Y_cr_back = Y_cr
                             
    Y_lvl70 = Y_mid + 0.723 * (Y_hip - Y_mid)
    Y_lvl715 = Y_mid + 0.788 * (Y_hip - Y_mid)
    Y_lvl5725 = (Y_lvl70 + Y_mid) / 2

    front_hip = H / 4 - 1                   
    back_hip = H / 4 + 1                   
    front_crotch = 0.25 * H - 2            
    back_crotch = 0.25 * H + 2             
    front_hem = 0.22 * H - 2               
    back_hem = 0.22 * H + 2                
    big_crotch = H / 10                    
    press = H / 5 - 1                         
    front_waist = W / 4 - 1 + 4.5               
    back_waist = W / 4 + 1 + 4.0               

                                   
    front = [
        (front_hem / 2 + 0.88, 0),                            
        (front_crotch / 2 + 1.00, Y_mid),                     
        (front_hip * 0.62, Y_lvl70 + 0.35),                   
        (front_hip * 0.652, Y_hip),                            
        (front_hip * 0.561, Y_w),                              
        (-front_hip * 0.393, Y_w),                             
        (-front_hip * 0.439, Y_hip),                           
        (-front_hip * 0.536, Y_lvl715),                        
        (-front_hip * 0.621, Y_lvl70 + 0.35),                  
        (-front_crotch / 2 - 1.00, Y_mid),                      
        (-front_hem / 2 - 0.88, 0),                             
    ]

                                       
    back = [
        (back_hem / 2 + 1.88, 0),                              
        (back_crotch / 2 + 1.00, Y_mid),                       
        (back_hip * 0.792, Y_hip),                             
        (back_hip * 0.795, Y_w),                               
        (-big_crotch * 0.361, Y_w + 1.99),                     
        (-big_crotch * 0.380, Y_w),                            
        (-back_hip * 0.292, Y_hip),                            
        (-press * 0.630, Y_lvl715),                            
        (-back_crotch * 0.759, Y_lvl70),                       
        (-back_crotch * 0.649, Y_lvl5725),                      
        (-back_crotch / 2 - 1.00, Y_mid),                       
        (-back_hem / 2 - 1.88, 0),                              
    ]

                           
    dart_center = W * 0.0883
    dart_w = 2.0
    dart_depth = 13.0
    back_dart = [
        (dart_center - dart_w / 2, Y_w),
        (dart_center + dart_w / 2, Y_w),
        (dart_center, Y_w - dart_depth),
    ]

    wb = [(0, 0), (W, 0), (W + 5, 0), (W + 6, wb_w / 2), (W + 5, wb_w), (W, wb_w), (0, wb_w)]

    return {
        "front": _round_pts(front),
        "back": _round_pts(back),
        "front_dart": [],
        "back_dart": [_round_pts(back_dart)],
        "waistband": _round_pts(wb),
        "y_lines": {"裤口线": 0, "中裆线": Y_mid, "前片横档线": Y_cr, "后片横档线": Y_cr_back, "臀围线": Y_hip, "腰围线": Y_w},
        "meta": {"成品腰围": W, "成品臀围": H, "总裤长": yao_gao + 1, "立裆": rise + wb_w},
    }


def get_nvxiku_baseline_error():
\
\
       
    p = NVXIKU_BASELINE_INPUT
    data = calc_nvxiku(p["W_s"], p["H_s"], p["yao_gao"], p["zuo_gao"])
    f_calc = data["front"]
    b_calc = data["back"]
    d_calc = data["back_dart"][0] if data.get("back_dart") else []

    def _err_list(calc_pts, base_pts):
        out = []
        for i, (cp, bp) in enumerate(zip(calc_pts, base_pts), 1):
            dx = _r2(cp[0] - bp[0])
            dy = _r2(cp[1] - bp[1])
            de = _r2(math.sqrt(dx * dx + dy * dy))
            out.append({"idx": i, "calc": cp, "base": bp, "dx": dx, "dy": dy, "dist": de})
        return out

    return {
        "front": _err_list(f_calc, NVXIKU_BASELINE_FRONT),
        "back": _err_list(b_calc, NVXIKU_BASELINE_BACK),
        "back_dart": _err_list(d_calc, NVXIKU_BASELINE_DART),
    }


                                                              
            
                                                              
def _build_outline(front_hem, front_hip, front_cr, front_waist,
                   back_hem, back_hip, back_cr, back_waist,
                   Y_w, Y_cr, Y_hip, Y_mid,
                   front_rise=1.0, back_tilt=1.5,
                   outer_ease_f=1.5, outer_ease_b=2.0,
                   inner_ease_f=1.5, inner_ease_b=2.0,
                   dart_w=2.0, dart_depth=13,
                   Y_cr_back=None):
\
\
\
\
       
    if Y_cr_back is None:
        Y_cr_back = Y_cr
    Y_sub = (Y_cr + Y_mid) / 2
    Y_sub_b = (Y_cr_back + Y_mid) / 2
    Y_hw = (Y_hip + Y_w) / 2        

                           
    cr_bot_xf = -front_cr - back_cr * 0.55
    sub_xf = (cr_bot_xf + (-front_hem)) / 2

    cr_bot_xb = -back_cr - back_cr * 0.6
    sub_xb = (cr_bot_xb + (-back_hem)) / 2

                                              
    front = [
        (front_hem, 0),
        (front_hem + outer_ease_f * 0.5, Y_mid * 0.4),
        (front_hem + outer_ease_f, Y_mid),
        ((front_hem + outer_ease_f + front_hip) / 2, (Y_mid + Y_hip) / 2),
        (front_hip, Y_hip),
        ((front_hip + front_waist) / 2 + 0.5, Y_hw),
        (front_waist, Y_w),
        (-front_cr * 0.3, Y_w + front_rise),
        (-front_cr - 0.5, Y_w),
        (-front_cr, (Y_hip + Y_w) / 2),
        (-front_cr, Y_hip),
        (-front_cr - 1.5, (Y_hip + Y_cr) / 2),
        (-front_cr - 3, Y_cr + 1),
        (cr_bot_xf, Y_cr),
        (sub_xf, Y_sub),
        (-front_hem, Y_mid),
        (-front_hem, Y_mid * 0.4),
        (-front_hem, 0),
    ]

                                              
    back = [
        (back_hem, 0),
        (back_hem + outer_ease_b * 0.5, Y_mid * 0.4),
        (back_hem + outer_ease_b, Y_mid),
        ((back_hem + outer_ease_b + back_hip) / 2, (Y_mid + Y_hip) / 2),
        (back_hip, Y_hip),
        ((back_hip + back_waist) / 2 + 0.5, Y_hw),
        (back_waist, Y_w),
        (-back_cr - 1, Y_w + back_tilt),
        (-back_cr, Y_hip + (Y_w - Y_hip) * 0.6),
        (-back_cr, Y_hip),
        (-back_cr - 1.5, (Y_hip + Y_cr_back) / 2),
        (-back_cr - 2, Y_cr_back + 1),
        (cr_bot_xb, Y_cr_back),
        (sub_xb, Y_sub_b),
        (-back_hem, Y_mid),
        (-back_hem, Y_mid * 0.4),
        (-back_hem, 0),
    ]

    dart_x = back_waist * 0.4
    dart_pts = [(dart_x - dart_w / 2, Y_w), (dart_x + dart_w / 2, Y_w), (dart_x, Y_w - dart_depth)]

    return front, back, dart_pts


                                                              
                                   
                                                              
def calc_zhitongku(W_s, H_s, yao_gao, zuo_gao):
    W = W_s + 4 + 2;  H = H_s + 14
    rise = zuo_gao + 1 + 5
    waist_band_w = 5
    pants_len = yao_gao - waist_band_w + 3
    Y_w = pants_len
    Y_cr = Y_w - rise
    Y_hip = Y_cr + rise / 3
    Y_mid = (Y_hip + 0) / 2 + 5
    SB = 0.2 * H + 6

    front_hip = H / 4 + 0.5
    back_hip  = H / 4 - 0.5
    front_cr  = 0.04 * H
    back_cr   = 0.11 * H
    front_hem = SB / 2 - 1
    back_hem  = SB / 2 + 1
    front_waist = W / 4 + 5
    back_waist  = W / 4 + 5

    front, back, dart = _build_outline(
        front_hem, front_hip, front_cr, front_waist,
        back_hem, back_hip, back_cr, back_waist,
        Y_w, Y_cr, Y_hip, Y_mid,
        front_rise=1.0, back_tilt=2.0,
        outer_ease_f=1.5, outer_ease_b=2.0,
        inner_ease_f=1.5, inner_ease_b=2.0,
        dart_w=2, dart_depth=13)

    wb = [(0,0),(W,0),(W+waist_band_w,0),(W+waist_band_w+1,waist_band_w/2),
          (W+waist_band_w,waist_band_w),(W,waist_band_w),(0,waist_band_w)]

    return {
        "front": front, "back": back, "back_dart": dart, "waistband": wb,
        "y_lines": {"裤口线": 0, "中裆线": Y_mid, "横裆线": Y_cr, "臀围线": Y_hip, "腰围线": Y_w},
        "meta": {"成品腰围": W, "成品臀围": H, "总裤长": pants_len+waist_band_w, "立裆": rise},
    }


                                                              
                  
                                                              
def calc_qianbiku(W_s, H_s, yao_gao, zuo_gao):
    H = H_s + 3;  W = W_s
    L = yao_gao - 8;  rise = zuo_gao + 2
    wb_w = 3; SB = 26
    Y_w = L
    Y_cr = Y_w - rise
    Y_hip = Y_cr + rise / 3
    Y_mid = Y_hip / 2 + 5

    front_hip = H/4 - 0.5
    back_hip  = H/4 + 0.5
    front_cr  = H/20 - 1
    back_cr   = H/10 - 1
    front_hem = SB/2 - 2
    back_hem  = SB/2 + 2
    front_waist = W/4 + 5
    back_waist  = W/4 + 5

    front, back, dart = _build_outline(
        front_hem, front_hip, front_cr, front_waist,
        back_hem, back_hip, back_cr, back_waist,
        Y_w, Y_cr, Y_hip, Y_mid,
        front_rise=2.0, back_tilt=1.5,
        outer_ease_f=2.0, outer_ease_b=2.5,
        inner_ease_f=2.0, inner_ease_b=2.5,
        dart_w=2, dart_depth=13)

    wb = [(0,0),(W,0),(W+wb_w,0),(W+wb_w+1,wb_w/2),(W+wb_w,wb_w),(W,wb_w),(0,wb_w)]

    return {
        "front": front, "back": back, "back_dart": dart, "waistband": wb,
        "y_lines": {"裤口线": 0, "中裆线": Y_mid, "横裆线": Y_cr, "臀围线": Y_hip, "腰围线": Y_w},
        "meta": {"成品腰围": W, "成品臀围": H, "总裤长": L+wb_w, "立裆": rise},
    }


                                                              
                          
                                                              
def calc_labaku(W_s, H_s, yao_gao, zuo_gao):
    H = H_s + 4;  W = W_s
    L = yao_gao - 3 + 1;  rise = zuo_gao + 2
    wb_w = 3; SB = 48
    Y_w = L
    Y_cr = Y_w - rise
    Y_hip = Y_cr + rise / 3
    Y_mid = Y_hip / 2 + 5
    Y_knee = Y_cr * 0.55

    front_hip = H/4 - 0.5
    back_hip  = H/4 + 0.5
    front_cr  = H/20 - 1
    back_cr   = H/10 - 1
    front_hem = SB/2 - 2
    back_hem  = SB/2 + 2
    front_waist = W/4 + 4
    back_waist  = W/4 + 4
                               
    front_knee = front_hip * 0.65
    back_knee  = back_hip * 0.65

    Y_sub = (Y_cr + Y_knee) / 2
    Y_hw = (Y_hip + Y_w) / 2
    Y_hk = (Y_knee + 0) / 2        

                                
    front = [
        (front_hem, 0),
        ((front_hem + front_knee) / 2, Y_hk),
        (front_knee, Y_knee),
        ((front_knee + front_hip) / 2 + 1, (Y_knee + Y_hip) / 2),
        (front_hip, Y_hip),
        ((front_hip + front_waist) / 2, Y_hw),
        (front_waist, Y_w),
        (-3.06, Y_w + 2),
        (-front_cr - 0.5, Y_w),
        (-front_cr, Y_hip),
        (-front_cr - 2, (Y_hip + Y_cr) / 2),
        (-front_cr - 3, Y_cr + 1),
        (-front_cr - back_cr * 0.5, Y_cr),
        ((-front_cr - back_cr * 0.5 + (-front_knee)) / 2, Y_sub),
        (-front_knee, Y_knee),
        ((-front_hem + -front_knee) / 2, Y_hk),
        (-front_hem, 0),
    ]
    back = [
        (back_hem, 0),
        ((back_hem + back_knee + 2) / 2, Y_hk),
        (back_knee + 2, Y_knee),
        ((back_knee + 2 + back_hip) / 2 + 1, (Y_knee + Y_hip) / 2),
        (back_hip, Y_hip),
        ((back_hip + back_waist) / 2, Y_hw),
        (back_waist, Y_w),
        (-back_cr - 1, Y_w + 2.5),
        (-back_cr, Y_hip + (Y_w - Y_hip) * 0.5),
        (-back_cr, Y_hip),
        (-back_cr - 1, (Y_hip + Y_cr) / 2),
        (-back_cr - 2, Y_cr + 1),
        (-back_cr - back_cr * 0.55, Y_cr),
        ((-back_cr - back_cr * 0.55 + (-back_knee - 2)) / 2, Y_sub),
        (-back_knee - 2, Y_knee),
        ((-back_hem + -back_knee - 2) / 2, Y_hk),
        (-back_hem, 0),
    ]
    dart_x = back_waist * 0.4
    dart = [(dart_x - 1, Y_w), (dart_x + 1, Y_w), (dart_x, Y_w - 10)]
    wb = [(0,0),(W,0),(W+wb_w,0),(W+wb_w+1,wb_w/2),(W+wb_w,wb_w),(W,wb_w),(0,wb_w)]

    return {
        "front": front, "back": back, "back_dart": dart, "waistband": wb,
        "y_lines": {"裤口线": 0, "膝线": Y_knee, "横裆线": Y_cr, "臀围线": Y_hip, "腰围线": Y_w},
        "meta": {"成品腰围": W, "成品臀围": H, "总裤长": L+wb_w, "立裆": rise},
    }


                                                              
                        
                                                              
def calc_niuzaiku(W_s, H_s, yao_gao, zuo_gao):
    H = H_s + 4;  W = W_s + 4
    L = yao_gao - 4;  rise = zuo_gao - 4
    wb_w = 4; SB = 40
    Y_w = L
    Y_cr = Y_w - rise
    Y_hip = Y_cr + rise / 3
    Y_mid = Y_hip / 2 + 6

    front_hip = H/4 - 1
    back_hip  = H/4 + 1
    front_cr  = H/20 - 1
    back_cr   = H/10 - 1.5
    front_hem = SB/2 - 2
    back_hem  = SB/2 + 2
    front_waist = W/4 - 1
    back_waist  = W/4 + 1

    front, back, dart = _build_outline(
        front_hem, front_hip, front_cr, front_waist,
        back_hem, back_hip, back_cr, back_waist,
        Y_w, Y_cr, Y_hip, Y_mid,
        front_rise=0.6, back_tilt=0.5,
        outer_ease_f=1.0, outer_ease_b=2.0,
        inner_ease_f=1.0, inner_ease_b=2.0,
        dart_w=2, dart_depth=11.5)

    wb = [(0,0),(W,0),(W+wb_w,0),(W+wb_w+1,wb_w/2),(W+wb_w,wb_w),(W,wb_w),(0,wb_w)]

    return {
        "front": front, "back": back, "back_dart": dart, "waistband": wb,
        "y_lines": {"裤口线": 0, "中裆线": Y_mid, "横裆线": Y_cr, "臀围线": Y_hip, "腰围线": Y_w},
        "meta": {"成品腰围": W, "成品臀围": H, "总裤长": L+wb_w, "立裆": rise+wb_w},
    }


                                                              
                 
                                                              
def calc_luoboku(W_s, H_s, yao_gao, zuo_gao):
    H = H_s + 12;  W = W_s + 4
    L = yao_gao - 4;  rise = zuo_gao - 0.5
    wb_w = 4; SB = 30
    Y_w = L
    Y_cr = Y_w - rise
    Y_hip = Y_cr + rise / 3
    Y_mid = Y_hip / 2 + 5

    front_hip = H/4 - 1
    back_hip  = H/4 + 1
    front_cr  = H/20
    back_cr   = H/10
    front_hem = SB/2 - 1
    back_hem  = SB/2 + 1
    front_waist = W/4 - 1 + 7.5
    back_waist  = W/4 + 1 + 5

                          
    front, back, dart = _build_outline(
        front_hem, front_hip, front_cr, front_waist,
        back_hem, back_hip, back_cr, back_waist,
        Y_w, Y_cr, Y_hip, Y_mid,
        front_rise=2.5, back_tilt=2.0,
        outer_ease_f=5.0, outer_ease_b=7.0,
        inner_ease_f=5.0, inner_ease_b=7.0,
        dart_w=2.5, dart_depth=14)

    wb = [(0,0),(W,0),(W+wb_w,0),(W+wb_w+1,wb_w/2),(W+wb_w,wb_w),(W,wb_w),(0,wb_w)]

    return {
        "front": front, "back": back, "back_dart": dart, "waistband": wb,
        "y_lines": {"裤口线": 0, "中裆线": Y_mid, "横裆线": Y_cr, "臀围线": Y_hip, "腰围线": Y_w},
        "meta": {"成品腰围": W, "成品臀围": H, "总裤长": L+wb_w, "立裆": rise},
    }


                                                              
                    
                                                              
def calc_sifenku(W_s, H_s, yao_gao, zuo_gao):
    H = H_s + 8;  W = W_s
    rise = zuo_gao - 4;  wb_w = 4
    SB = 54
    L = 36
    Y_w = L
    Y_cr = Y_w - rise
    Y_hip = Y_cr + rise / 3

    front_hip = H/4
    back_hip  = H/4
    front_cr  = H/20 - 1
    back_cr   = H/10
    front_hem = SB/2 - 2
    back_hem  = SB/2 + 2
    front_waist = W/4 + 5
    back_waist  = W/4 + 4

    Y_hw = (Y_hip + Y_w) / 2

                             
    front = [
        (front_hem, 0),
        ((front_hem + front_hip) / 2, Y_hip * 0.4),
        (front_hip, Y_hip),
        ((front_hip + front_waist) / 2, Y_hw),
        (front_waist, Y_w),
        (-front_cr * 0.3, Y_w + 1),
        (-front_cr, Y_w),
        (-front_cr, Y_hip),
        (-front_cr - 3, Y_cr + 1),
        (-front_cr - back_cr * 0.5, Y_cr),
        (-front_hem, 0),
    ]
    back = [
        (back_hem, 0),
        ((back_hem + back_hip) / 2 + 1, Y_hip * 0.4),
        (back_hip + 1, Y_hip),
        ((back_hip + 1 + back_waist) / 2, Y_hw),
        (back_waist, Y_w),
        (-back_cr - 1, Y_w + 2),
        (-back_cr, Y_hip),
        (-back_cr - 2, Y_cr + 1),
        (-back_cr - back_cr * 0.5, Y_cr),
        (-back_hem, 0),
    ]
    dart_x = back_waist * 0.35
    dart = [(dart_x - 1.25, Y_w), (dart_x + 1.25, Y_w), (dart_x, Y_w - 10)]
    wb = [(0,0),(W,0),(W+wb_w,0),(W+wb_w+1,wb_w/2),(W+wb_w,wb_w),(W,wb_w),(0,wb_w)]

    return {
        "front": front, "back": back, "back_dart": dart, "waistband": wb,
        "y_lines": {"裤口线": 0, "横裆线": Y_cr, "臀围线": Y_hip, "腰围线": Y_w},
        "meta": {"成品腰围": W, "成品臀围": H, "总裤长": L+wb_w, "立裆": rise+wb_w},
    }


                                                              
                 
                                                              
def calc_yunfuku(W_s, H_s, yao_gao, zuo_gao):
    H = H_s + 18;  W = W_s
    L = yao_gao - 4;  rise = zuo_gao + 2
    wb_w = 4; SB = 40
    Y_w = L
    Y_cr = Y_w - rise
    Y_hip = Y_cr + rise / 3
    Y_mid = Y_hip / 2 + 5

    front_hip = H/4 - 1
    back_hip  = H/4 + 1
    front_cr  = H/20 - 1
    back_cr   = H/10
    front_hem = SB/2 - 2
    back_hem  = SB/2 + 2
    front_waist = W/4 + 9
    back_waist  = W/4 + 5

    front, back, dart = _build_outline(
        front_hem, front_hip, front_cr, front_waist,
        back_hem, back_hip, back_cr, back_waist,
        Y_w, Y_cr, Y_hip, Y_mid,
        front_rise=1.5, back_tilt=1.0,
        outer_ease_f=3.0, outer_ease_b=4.0,
        inner_ease_f=3.0, inner_ease_b=4.0,
        dart_w=2, dart_depth=14)

                   
    belly_h = 19
    wb = [(0,0),(W,0),(W+wb_w,0),(W+wb_w,belly_h*0.4),
          (W*0.7,belly_h),(W*0.3,belly_h),(0,belly_h*0.8)]

    return {
        "front": front, "back": back, "back_dart": dart, "waistband": wb,
        "y_lines": {"裤口线": 0, "中裆线": Y_mid, "横裆线": Y_cr, "臀围线": Y_hip, "腰围线": Y_w},
        "meta": {"成品腰围": W, "成品臀围": H, "总裤长": L+wb_w, "立裆": rise},
    }


                                                              
                 
                                                              
def calc_xiuxianku(W_s, H_s, yao_gao, zuo_gao):
    H = H_s + 4;  W = W_s
    L = yao_gao - 3 + 1;  rise = zuo_gao + 2
    wb_w = 3; SB = 48
    Y_w = L
    Y_cr = Y_w - rise
    Y_hip = Y_cr + rise / 3
    Y_mid = Y_hip / 2 + 5

    front_hip = H/4 - 0.5
    back_hip  = H/4 + 0.5
    front_cr  = H/20 - 1
    back_cr   = H/10 - 1
    front_hem = SB/2 - 2
    back_hem  = SB/2 + 2
    front_waist = W/4 + 3
    back_waist  = W/4 + 3

    front, back, dart = _build_outline(
        front_hem, front_hip, front_cr, front_waist,
        back_hem, back_hip, back_cr, back_waist,
        Y_w, Y_cr, Y_hip, Y_mid,
        front_rise=1.0, back_tilt=1.5,
        outer_ease_f=1.0, outer_ease_b=2.0,
        inner_ease_f=1.0, inner_ease_b=2.0,
        dart_w=3, dart_depth=14)

    wb = [(0,0),(W,0),(W+wb_w,0),(W+wb_w+1,wb_w/2),(W+wb_w,wb_w),(W,wb_w),(0,wb_w)]

    return {
        "front": front, "back": back, "back_dart": dart, "waistband": wb,
        "y_lines": {"裤口线": 0, "中裆线": Y_mid, "横裆线": Y_cr, "臀围线": Y_hip, "腰围线": Y_w},
        "meta": {"成品腰围": W, "成品臀围": H, "总裤长": L+wb_w, "立裆": rise},
    }


                                                              
      
                                                              
CALC_MAP = {
    "女西裤": calc_nvxiku,
    "直筒裤": calc_zhitongku,
    "铅笔裤": calc_qianbiku,
    "喇叭裤": calc_labaku,
    "牛仔裤": calc_niuzaiku,
    "萝卜裤": calc_luoboku,
    "四分裤": calc_sifenku,
    "孕妇裤": calc_yunfuku,
    "休闲裤": calc_xiuxianku,
}

def calculate_pants(pants_type, W_star, H_star, yao_gao, zuo_gao):
    fn = CALC_MAP.get(pants_type)
    if fn is None:
        raise ValueError(f"未知裤型: {pants_type}")
    data = fn(W_star, H_star, yao_gao, zuo_gao)
                                                     
    for key in ("front_dart", "back_dart"):
        darts = data.get(key, [])
        if darts and isinstance(darts, list) and len(darts) == 3 and isinstance(darts[0], tuple):
            data[key] = [darts]
        data.setdefault(key, [])
    return data
