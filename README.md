##Screen Shot
![shot](http://7xiqgb.com1.z0.glb.clouddn.com/loadingdrawable.gif)

##[GitHub地址](https://github.com/JingHaifeng/LoadingDrawable)

##Useage
`LoadingState`

	public enum LoadingState {
        LOADING, ERROR, SUCCESS
    }
	
	
`init`

	LoadingDrawable drawable = new LoadingDrawable(this);
	drawable.setBorder();
	drawable.setMaxSweepAngle();
	drawable.setMinSweepAngle();
	
`setState`

	drawable.setLoadingState();

####[New Blog](www.jinghaifeng.com)
