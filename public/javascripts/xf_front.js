
$(function(){	

	$("p").limit();	//截取字数 
	/****************导航栏效果切换*********************/
	$(".xf_h_idx_1_navul li").click(function(){
		$(this).addClass("xf_h_idx_1_navul_hover").siblings().removeClass("xf_h_idx_1_navul_hover");
	});	
	/****************导航栏效果切换*********************/
	
	
	/***banner 滑动 start ***/			
	$("#xf_b_idx_2_banner").css("width",$(window).width()+"px");
	$("#xf_b_idx_2_banner ul li").css("width",$(window).width()+"px");
	var sWidth = $("#xf_b_idx_2_banner").outerWidth(); 
	var len = $("#xf_b_idx_2_banner ul li").length;
	var index = 0;
	var picTimer; 
			
	var btn = "<div class='btnBg'></div><div class='btn'>";
	for(var i=0; i < len; i++) {
		btn += "<span></span>";
	}
	btn += "</div>";
	$("#xf_b_idx_2_banner").append(btn);
	$("#xf_b_idx_2_banner .btnBg").css("opacity",1);
			
	$(".btn").css("left",($(window).width()-$(".btn").outerWidth())/2+"px");
	//为小按钮添加鼠标滑入事件，以显示相应的内容
	$("#xf_b_idx_2_banner .btn span").css("opacity",1).mouseover(function() {
		index = $("#xf_b_idx_2_banner .btn span").index(this);
		showPics(index);
	}).eq(0).trigger("mouseover");

			
	$("#xf_b_idx_2_banner ul").css("width",sWidth * (len));
			
		//鼠标滑上焦点图时停止自动播放，滑出时开始自动播放
		$("#xf_b_idx_2_banner").hover(function() {
			clearInterval(picTimer);
		},function() {
			picTimer = setInterval(function() {
				showPics(index);
				index++;
				if(index == len) {index = 0;}
			},3000); 
		}).trigger("mouseleave");
			
			//显示图片函数，根据接收的index值显示相应的内容
	function showPics(index) { 
		var nowLeft = -index*sWidth;
		$("#xf_b_idx_2_banner ul").stop(true,false).animate({"left":nowLeft},300); 
		$("#xf_b_idx_2_banner .btn span").removeClass("active").eq(index).addClass("active"); 
		$("#xf_b_idx_2_banner .btn span").stop(true,false).animate({"opacity":"1"},300).eq(index).stop(true,false).animate({"opacity":"1"},300);
	}	
	
	/******banner 滑动 ends ******/	

	$(".xf_c_idx_10_sliderUl").css("width",$(".xf_c_idx_10_sliderUl li").outerWidth() * $(".xf_c_idx_10_sliderUl li").length + "");
	
	//我要借款详情页选项卡
	$(".xf_con_wyjk_tentultbody:eq(0)").show();
	$("#xf_wyjkview_xxk").children("ul").find("li").click(function(){
		var xxkli = $(this).index();
		$(this).addClass("xf_con_wyjk_r_liishot").siblings().removeClass("xf_con_wyjk_r_liishot");
		$(".xf_con_wyjk_tentultbody").eq(xxkli).show().siblings(".xf_con_wyjk_tentultbody").hide();

	});

	//我的账户左侧导航效果
	$("#xf_mem_nav_left .xf_mem_l_title_zhz").hide();
	$("#xf_mem_nav_left .xf_mem_r_more").click(function(){
		var ernav = $(this).siblings(".xf_mem_l_title_zhz").is(":hidden"),
			ernav2 = $(this).siblings(".xf_mem_l_title_zhz").is(":visible")
		if(ernav){
			$(this).siblings(".xf_mem_l_title_zhz").show();
			$(this).parent().addClass("xf_con_wyjk_leftliisshow");
			$(this).addClass("xf_mem_r_jian");
		}
		if(ernav2){
			$(this).siblings(".xf_mem_l_title_zhz").hide();
			$(this).parent().removeClass("xf_con_wyjk_leftliisshow");
			$(this).removeClass("xf_mem_r_jian");
		}
	});

	//选项卡
	$(".xf_ht_Tab:eq(0)").show();
	$("#xf_wyjkview_xxk").children("ul").find("li").click(function(){
		var xxkli = $(this).index();
		$(this).addClass("xf_con_wyjk_r_liishot").siblings().removeClass("xf_con_wyjk_r_liishot");
		$(".xf_ht_Tab").eq(xxkli).show().siblings(".xf_ht_Tab").hide();
	});

	// 详情页折叠
	var tabH = $(".xf_con_mem_r_bottom").height();
	$(".xf_mem_r_b_title_x").click(function(){
		if ( $(this).parents(".xf_con_mem_r_bottom").height() > 45 ) {
			$(this).addClass("xf_mem_r_b_title_x2");
			$(this).parents(".xf_mem_r_b_title_th").addClass("xf_mem_r_b_title_th2");
			$(this).parents(".xf_con_mem_r_bottom").animate({height:"45px"});
		} else{
			$(this).removeClass("xf_mem_r_b_title_x2");
			$(this).parents(".xf_con_mem_r_bottom").animate({height:tabH});
		};
	});

	//表格最后一列去除边框
	$(".xfht_t_table_center tr").each(function(){
		$(this).find("td:last").css("borderRight","0");
		$(this).find("th:last").css("borderRight","0");
	});
});

$(document).ready(function(e) { 
	setInterval(GetWindowWidth,10); 
	}); 
	function GetWindowWidth(){ 
	var w = $("#xf_b_idx_2_banner").innerWidth(); 
	if (w < 1920){ 
	$("#xf_b_idx_2_banul a img").css({"left":(w-1920)/2+"px"}); 
	}else{ 
	$("#xf_b_idx_2_banul a img").css({"left":0+"px"}); 
	} 
}


	