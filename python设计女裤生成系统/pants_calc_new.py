\
\
\
   


def calc_zhitongku(W=68, H=94, L=98, SZ=27, JKW=48, YTW=3):
                             
    PL = L - YTW + 1
    SD = SZ - YTW + 1
    y_w = PL;  y_h = PL - SZ/3;  y_d = PL - SD;  y_m = (y_h)/2 + 5
    fh = H/4-0.5;  bh = H/4+0.5
    fw = W/4 + 2.06;  bw = W/4 + 2.06
    fd = H/20-1;  bd = H/10-1
    fk = JKW/2-2;  bk = JKW/2+2
    fm = 0.25*H;  bm = 0.25*H
    front = [(fk/2,0),(fm/2,y_m),(fh/2,y_d),(fh/2,y_h),(fw/2,y_w),
             (-fw/2,y_w),(-fh/2,y_h),(-(fh/2+fd),y_d),(-fm/2,y_m),(-fk/2,0)]
    back = [(bk/2,0),(bm/2,y_m),(bh/2,y_d),(bh/2,y_h),(bw/2,y_w),
            (-bw/2,y_w),(-bh/2,y_h),(-(bh/2+bd),y_d),(-bm/2,y_m),(-bk/2,0)]
    bd_dart = [[(-bw/4,y_w),(bw/4,y_w),(0,y_h-4)]]
    fd_dart = [[(fw/4-0.5,y_w),(fw/4+1.5,y_w),(fw/4+0.5,y_w-10)]]
    wb = [(0,0),(W,0),(W,YTW),(0,YTW)]
    yl = {"裤口线":0,"中裆线":r2(y_m),"裆底线":r2(y_d),"臀围线":r2(y_h),"腰围线":r2(y_w)}
    return _pack("女标准直筒西裤","zhitongku",front,back,fd_dart,bd_dart,wb,yl,
                 W,H,L,SZ,{"坐高":SZ-3,"腰高":L-1,"腰围":W,"臀围":H})


def calc_qianbiku(W=68, H=93, L=90, SZ=27, JKW=26, YTW=3):
                             
    PL = L - YTW + 1
    SD = SZ - YTW + 1
    y_w = PL;  y_h = PL - SZ/3;  y_d = PL - SD;  y_m = (y_h)/2 + 5
    fh = H/4-0.5;  bh = H/4+0.5
    fw = W/4 + 2.8;  bw = W/4 + 2.8
    fd = H/20-1;  bd = H/10-1
    fk = JKW/2-2;  bk = JKW/2+2
    fm = 0.25*H-2;  bm = 0.25*H+2
    front = [(fk/2,0),(fm/2,y_m),(fh/2,y_d),(fh/2,y_h),(fw/2,y_w),
             (-fw/2,y_w),(-fh/2,y_h),(-(fh/2+fd),y_d),(-fm/2,y_m),(-fk/2,0)]
    back = [(bk/2,0),(bm/2,y_m),(bh/2,y_d),(bh/2,y_h),(bw/2,y_w),
            (-bw/2,y_w),(-bh/2,y_h),(-(bh/2+bd),y_d),(-bm/2,y_m),(-bk/2,0)]
    bd_dart = [[(-bw/4,y_w),(bw/4,y_w),(0,y_h-4)]]
    fd_dart = [[(fw/4-0.5,y_w),(fw/4+1.5,y_w),(fw/4+0.5,y_w-10)]]
    wb = [(0,0),(W,0),(W,YTW),(0,YTW)]
    yl = {"裤口线":0,"中裆线":r2(y_m),"裆底线":r2(y_d),"臀围线":r2(y_h),"腰围线":r2(y_w)}
    return _pack("女紧身铅笔裤","qianbiku",front,back,fd_dart,bd_dart,wb,yl,
                 W,H,L,SZ,{"坐高":SZ-3,"腰高":L-1,"腰围":W,"臀围":H})


def calc_labaku(W=68, H=94, L=98, SZ=27, JKW=48, YTW=3):
                      
    PL = L - YTW + 1
    SD = SZ - YTW + 1
    y_w = PL;  y_h = PL - SZ/3;  y_d = PL - SD;  y_m = (y_h)/2 + 5
    fh = H/4-0.5;  bh = H/4+0.5
    fw = W/4 + 3.06;  bw = W/4 + 3.06
    fd = H/20-1;  bd = H/10-1
    fk = JKW/2-2;  bk = JKW/2+2
    fm = 0.25*H-2;  bm = 0.25*H+2
    front = [(fk/2,0),(fm/2,y_m),(fh/2,y_d),(fh/2,y_h),(fw/2,y_w),
             (-fw/2,y_w),(-fh/2,y_h),(-(fh/2+fd),y_d),(-fm/2,y_m),(-fk/2,0)]
    back = [(bk/2,0),(bm/2,y_m),(bh/2,y_d),(bh/2,y_h),(bw/2,y_w),
            (-bw/2,y_w),(-bh/2,y_h),(-(bh/2+bd),y_d),(-bm/2,y_m),(-bk/2,0)]
    bd_dart = [[(-bw/4,y_w),(bw/4,y_w),(0,y_h-4)]]
    fd_dart = [[(fw/4-0.5,y_w),(fw/4+1.5,y_w),(fw/4+0.5,y_w-10)]]
    wb = [(0,0),(W,0),(W,YTW),(0,YTW)]
    yl = {"裤口线":0,"中裆线":r2(y_m),"裆底线":r2(y_d),"臀围线":r2(y_h),"腰围线":r2(y_w)}
    return _pack("女喇叭裤","labaku",front,back,fd_dart,bd_dart,wb,yl,
                 W,H,L,SZ,{"坐高":SZ-3,"腰高":L-1,"腰围":W,"臀围":H})


def calc_niuzaiku(W=72, H=94, L=100, SZ=25, JKW=40, YTW=4):
                             
    PL = L - YTW
    SD = SZ - YTW
    y_w = PL;  y_h = PL - SZ/3;  y_d = PL - SD;  y_m = (y_h)/2 + 6
    fh = H/4-1;  bh = H/4+1
    fw = W/4 - 1;  bw = W/4 + 1
    fd = H/20-1;  bd = H/10-1.5
    fk = JKW/2-2;  bk = JKW/2+2
    fm = 0.25*H;  bm = 0.25*H
    front = [(fk/2,0),(fm/2,y_m),(fh/2,y_d),(fh/2,y_h),(fw/2,y_w),
             (-fw/2,y_w),(-fh/2,y_h),(-(fh/2+fd),y_d),(-fm/2,y_m),(-fk/2,0)]
    back = [(bk/2,0),(bm/2,y_m),(bh/2,y_d),(bh/2,y_h),(bw/2,y_w),
            (-bw/2,y_w),(-bh/2,y_h),(-(bh/2+bd),y_d),(-bm/2,y_m),(-bk/2,0)]
    bd_dart = [[(-bw/4,y_w),(bw/4,y_w),(0,y_h-4)]]
    fd_dart = [[(1.5,y_w),(4.5,y_w),(3.0,y_w-10)]]
    wb = [(0,0),(W,0),(W,YTW),(0,YTW)]
    yl = {"裤口线":0,"中裆线":r2(y_m),"裆底线":r2(y_d),"臀围线":r2(y_h),"腰围线":r2(y_w)}
    return _pack("女牛仔裤","niuzaiku",front,back,fd_dart,bd_dart,wb,yl,
                 W,H,L,SZ,{"坐高":SZ-3,"腰高":L-1,"腰围":W,"臀围":H})


def calc_luoboku(W=72, H=102, L=104, SZ=28.5, JKW=30, YTW=4):
                      
    PL = L - YTW
    SD = SZ - YTW
    y_w = PL;  y_h = PL - SZ/3;  y_d = PL - SD;  y_m = (y_h)/2 + 5
    fh = H/4-1;  bh = H/4+1
    fw = W/4-1+7.5;  bw = W/4+1+5
    fd = H/20-1;  bd = H/10
    fk = JKW/2-1;  bk = JKW/2+1
    fm = (fh+fk)/2;  bm = (bh+bk)/2
    front = [(fk/2,0),(fm/2,y_m),(fh/2,y_d),(fh/2,y_h),(fw/2,y_w),
             (-fw/2,y_w),(-fh/2,y_h),(-(fh/2+fd),y_d),(-fm/2,y_m),(-fk/2,0)]
    back = [(bk/2,0),(bm/2,y_m),(bh/2,y_d),(bh/2,y_h),(bw/2,y_w),
            (-bw/2,y_w),(-bh/2,y_h),(-(bh/2+bd),y_d),(-bm/2,y_m),(-bk/2,0)]
    bd_dart = [[(-3,y_w),(3,y_w),(0,y_h-3.5)]]
    fd_dart = [[(1.8,y_w),(5.6,y_w),(3.7,y_w-10)],[(6.1,y_w),(9.8,y_w),(8.0,y_w-10)]]
    wb = [(0,0),(W,0),(W,YTW),(0,YTW)]
    yl = {"裤口线":0,"中裆线":r2(y_m),"裆底线":r2(y_d),"臀围线":r2(y_h),"腰围线":r2(y_w)}
    return _pack("女萝卜裤","luoboku",front,back,fd_dart,bd_dart,wb,yl,
                 W,H,L,SZ,{"坐高":SZ-3,"腰高":L-1,"腰围":W,"臀围":H})


def calc_sifenku(W=68, H=98, L=40, SZ=28, JKW=54, YTW=4):
                                                          
                                           
    PL = L - YTW
    SD = SZ - YTW
    y_w = PL
    y_d = PL - SD
                                     
    y_h = y_d + SD / 3
    fh = H / 4
    bh = H / 4
    fw = W / 4 + 5
    bw = W / 4 + 4
    fd = H / 20 - 1
    bd = H / 10
                                                     
    hem_f = (JKW / 2 - 2) / 2
    hem_b = (JKW / 2 + 2) / 2
                    
    hip_out = 0.8
    x_f_leg_d = fh / 2 - 0.75
    x_f_hip = fh / 2 - 0.75 + hip_out
    x_f_waist = fw / 2 + 0.5
    x_f_crotch_in = -(fh / 2 + fd - 0.5)
                           
                                        
    x_f_in_mid1 = (-x_f_leg_d + x_f_crotch_in) / 2
    y_f_in_mid1 = (y_h + y_d) / 2
    x_f_in_mid2 = (x_f_crotch_in - hem_f) / 2
    y_f_in_mid2 = y_d / 2
    front = [
        (hem_f, 0),
        (x_f_leg_d, y_d),
        (x_f_hip, y_h),
        (x_f_waist, y_w),
        (-x_f_waist, y_w - 1),
        (-x_f_leg_d, y_h),
        (x_f_in_mid1, y_f_in_mid1),
        (x_f_crotch_in, y_d),
        (x_f_in_mid2, y_f_in_mid2),
        (-hem_f, 0),
    ]
    x_b_leg_d = bh / 2 + 0.5
    x_b_hip = bh / 2 + 0.5
    x_b_waist = bw / 2 + 0.5
                                     
    x_b_crotch_in = -(bh / 2 + bd * 0.56 + 1.0)
    x_b_in_mid1 = (-x_b_hip + x_b_crotch_in) / 2
    y_b_in_mid1 = (y_h + y_d) / 2
    x_b_in_mid2 = (x_b_crotch_in - hem_b) / 2
    y_b_in_mid2 = y_d / 2
    back = [
        (hem_b, 0),
        (x_b_leg_d, y_d),
        (x_b_hip, y_h),
        (x_b_waist, y_w + 2),
        (-x_b_waist, y_w),
        (-x_b_hip, y_h),
        (x_b_in_mid1, y_b_in_mid1),
        (x_b_crotch_in, y_d),
        (x_b_in_mid2, y_b_in_mid2),
        (-hem_b, 0),
    ]
    sw = W / 68.0
                                         
    fd_dart = [
        [
            (fw / 2 - 5.5, y_w),
            (fw / 2 - 3.0, y_w),
            (fw / 2 - 4.25, y_w - 15),
        ],
        [
            (-3.5 * sw, y_w - 1),
            (-1.0 * sw, y_w - 1),
            (-2.25 * sw, y_w - 11),
        ],
    ]
                                  
    y_bw = y_w + 1
    bd_dart = [
        [(-6.0 * sw, y_bw), (-4.0 * sw, y_bw), (-5.0 * sw, y_bw - 11)],
        [(3.0 * sw, y_bw), (5.0 * sw, y_bw), (4.0 * sw, y_bw - 10)],
    ]
                             
    wb = [(0, 0), (W, 0), (W + 5, 0), (W + 6, YTW / 2), (W + 5, YTW), (W, YTW), (0, YTW)]
    yl = {"裤口线": 0, "裆底线": r2(y_d), "臀围线": r2(y_h), "腰围线": r2(y_w)}
    return _pack("女四分短裤","sifenku",front,back,fd_dart,bd_dart,wb,yl,
                 W,H,L,SZ,{"坐高":SZ-3,"腰高":L-1,"腰围":W,"臀围":H},
                 draw_mode="sifenku")


def calc_yunfuku(W=68, H=108, L=100, SZ=31, JKW=40, YTW=4):
                      
    PL = L - YTW
    SD = SZ - YTW
    y_w = PL;  y_h = PL - SZ/3;  y_d = PL - SD;  y_m = (y_h)/2 + 5
    fh = H/4-1;  bh = H/4+1
    fw = W/4+9;  bw = W/4+5
    fd = H/20-1;  bd = H/10
    fk = JKW/2-2;  bk = JKW/2+2
    fm = 0.25*H-2;  bm = 0.25*H+2
    front = [(fk/2,0),(fm/2,y_m),(fh/2,y_d),(fh/2,y_h),(fw/2,y_w),
             (-fw/2,y_w),(-fh/2,y_h),(-(fh/2+fd),y_d),(-fm/2,y_m),(-fk/2,0)]
    back = [(bk/2,0),(bm/2,y_m),(bh/2,y_d),(bh/2,y_h),(bw/2,y_w),
            (-bw/2,y_w),(-bh/2,y_h),(-(bh/2+bd),y_d),(-bm/2,y_m),(-bk/2,0)]
    bd_dart = [[(-3,y_w),(3,y_w),(0,y_h-3.5)]]
    fd_dart = [[(2,y_w),(6.5,y_w),(4.25,y_w-8)],[(7,y_w),(11.5,y_w),(9.25,y_w-8)]]
    wb = [(0,0),(W,0),(W,YTW),(0,YTW)]
    yl = {"裤口线":0,"中裆线":r2(y_m),"裆底线":r2(y_d),"臀围线":r2(y_h),"腰围线":r2(y_w)}
    return _pack("孕妇托腹裤","yunfuku",front,back,fd_dart,bd_dart,wb,yl,
                 W,H,L,SZ,{"坐高":SZ-3,"腰高":L-1,"腰围":W,"臀围":H})


def calc_xiuxianku(W=74, H=104, L=98, SZ=31, JKW=None, YTW=5):
                             
    SB = 0.2*H+6 if JKW is None else JKW
    PL = L
    SD = SZ
    y_w = PL;  y_h = PL - SZ/3;  y_d = PL - SD;  y_m = (y_h)/2 + 5
    fh = H/4+0.5;  bh = H/4-0.5
    fw = W/4+3.5;  bw = W/4+3.5
    fd = 0.04*H;  bd = 0.11*H
    fk = SB/2-1;  bk = SB/2+1
    fm = 0.25*H;  bm = 0.25*H
    front = [(fk/2,0),(fm/2,y_m),(fh/2,y_d),(fh/2,y_h),(fw/2,y_w),
             (-fw/2,y_w),(-fh/2,y_h),(-(fh/2+fd),y_d),(-fm/2,y_m),(-fk/2,0)]
    back = [(bk/2,0),(bm/2,y_m),(bh/2,y_d),(bh/2,y_h),(bw/2,y_w),
            (-bw/2,y_w),(-bh/2,y_h),(-(bh/2+bd),y_d),(-bm/2,y_m),(-bk/2,0)]
    bd_dart = [[(-3,y_w),(3,y_w),(0,y_h-3)]]
    fd_dart = [[(fw/4-0.5,y_w),(fw/4+1.5,y_w),(fw/4+0.5,y_w-10)]]
    wb = [(0,y_w),(W,y_w),(W,y_w+YTW),(0,y_w+YTW)]
    yl = {"裤口线":0,"中裆线":r2(y_m),"裆底线":r2(y_d),"臀围线":r2(y_h),"腰围线":r2(y_w)}
    return _pack("宽松连腰直筒休闲裤","xiuxianku",front,back,fd_dart,bd_dart,wb,yl,
                 W,H,L,SZ,{"坐高":SZ-3,"腰高":L-1,"腰围":W,"臀围":H})


def r2(v):
    return round(v, 2)


def _pack(name, fn, front, back, fd, bd, wb, yl, W, H, L, SZ, std, draw_mode=None):
    d = {
        "name": name, "filename": fn,
        "front": [(r2(x),r2(y)) for x,y in front],
        "back": [(r2(x),r2(y)) for x,y in back],
        "front_dart": [[(r2(x),r2(y)) for x,y in dd] for dd in fd],
        "back_dart": [[(r2(x),r2(y)) for x,y in dd] for dd in bd],
        "waistband": [(r2(x),r2(y)) for x,y in wb],
        "y_lines": yl,
        "meta": {"成品腰围":W,"成品臀围":H,"总裤长":L,"立裆":SZ},
        "std_input": std,
    }
    if draw_mode:
        d["draw_mode"] = draw_mode
    return d


CALC_MAP = {
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
\
\
\
\
       
    fn = CALC_MAP.get(pants_type)
    if fn is None:
        raise ValueError(f"未知裤型: {pants_type}")
    return fn(W=W_star, H=H_star, L=yao_gao + 1, SZ=zuo_gao + 3)


if __name__ == "__main__":
    for name, fn in CALC_MAP.items():
        d = fn()
        print(f"\n=== {name} ===")
        print(f"  front({len(d['front'])}): {d['front']}")
        print(f"  back({len(d['back'])}):  {d['back']}")
        print(f"  y_lines: {d['y_lines']}")
