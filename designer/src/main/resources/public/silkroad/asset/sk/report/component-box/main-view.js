define(["template","dialog","report/component-box/main-model","report/component-box/main-template"],function(a,b,c,d){return Backbone.View.extend({events:{"change #component-group-selector":"showCompByGroup","mouseover .j-component-border":"showEditorLine","mouseout .j-component-border":"hideEditorLine"},initialize:function(a){this.model=new c({id:this.id}),this.canvasView=a.canvasView,this.$el.find(".component-menu").append(d.render(this.model.config)),this.showCompByGroup(),this.initDrag()},showCompByGroup:function(){var a=this.$el.find(".j-con-component-box"),b=""+a.find("#component-group-selector").val(),c=".j-con-component[data-group-id="+b+"]";a.find(c).show(),this.currentGroupId=b},initDrag:function(){var a=this,b=".j-report",c=0,d=a.$el.find(b);$(".j-con-component .j-component-item",this.$el).draggable({appendTo:b,helper:"clone",scroll:!0,scrollSensitivity:100,opacity:1,drag:function(){},start:function(b,e){var f=e.helper.attr("data-component-type"),g=a.model.getComponentData(f);c=d.scrollTop(),$(".j-all-menus").hide(),e.helper.html("临时展示").css({width:"100px",height:"100px",cursor:"move"}).addClass("active shell-component j-component-border"),e.helper.attr("data-default-width",g.defaultWidth),e.helper.attr("data-default-height",g.defaultHeight),e.helper.attr("data-sort-startScrrolTop",e.helper.parent().scrollTop()),$(".j-report").addClass("active")}})},showEditorLine:function(){var a=this.$el.find($(".j-component-border"));a.mouseover(function(){$(this).find(".con-edit-btns").children().show(),$(this).css("padding-top","0"),$(this).find(".j-guide-line").show()})},hideEditorLine:function(){var a=this.$el.find($(".j-component-border"));a.mouseout(function(){$(this).find(".con-edit-btns").children().hide(),$(this).css("padding-top","20px"),$(this).find(".j-guide-line").hide()})},destroy:function(){this.model.clear({silent:!0}),this.stopListening(),this.$el.unbind().empty()}})});