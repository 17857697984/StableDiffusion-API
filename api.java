public String createWithText(String keyWord,int status){
        Map<String,String> param = new HashMap<>();
        param.put("prompt","wuchangshuo,"+keyWord);
        param.put("sampler_index", "Euler a");
        param.put("steps","20");
        param.put("tiling",status == 0?"true":"false");

        RestTemplate template = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type","application/json");
        HttpEntity<String> params = new HttpEntity<>(JSON.toJSONString(param),headers);
        ResponseEntity<String> response = null;
        try{
            response = template.exchange(createHostUrl + "/sdapi/v1/txt2img", HttpMethod.POST,params,String.class);
        }catch (Exception e){
            System.out.println("sd请求失败"+e);
        }
        String base64String = JSON.parseObject(response.getBody()).getString("images");
        base64String = base64String.replace("[\"","");
        base64String = base64String.replace("\"]","");
        return getImageUrl(base64String);
    }
    public String createWithImage(String keyWord,String style,int status,double similarityValue,int imageId){
        //1.根据图片id获得图片路径
        LambdaQueryWrapper<MyFodder> lqw = new LambdaQueryWrapper<>();
        lqw.eq(MyFodder::getId,imageId);
        MyFodder myFodder = myFodderDao.selectOne(lqw);
        String sourceImagePath = myFodder.getSrc();
        if(imageId >0){
            //1.如果是我的素材，则把url前缀去掉
            sourceImagePath = ImageUtil.deleteURLPath(myFodder.getSrc());
            //2.加上文件路径
            sourceImagePath = "***" + sourceImagePath;
        }else{
            //加上文件路径
            sourceImagePath = "***" + sourceImagePath;
        }
        //2.调用模型进行生成，返回图片路径
        Map<String,Object> param = new HashMap<>();
//        String initImages = null;
        List<String> initImages = new ArrayList<>();
        try {
            initImages.add(encodeFileToBase64(sourceImagePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        param.put("prompt","wuchangshuo,"+keyWord);
        param.put("sampler_index", "Euler a");
        param.put("steps","20");
        param.put("tiling",status == 0?"true":"false");
        param.put("init_images",initImages);
        param.put("denoising_strength", similarityValue);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type","application/json");
        HttpEntity<String> params = new HttpEntity<>(JSON.toJSONString(param),headers);
        //3.得到了新生成图片的路径，在将其访问url返回
        RestTemplate template = new RestTemplate();
        ResponseEntity<String> response = null;
        try{
            response = template.exchange(createHostUrl + "/sdapi/v1/img2img", HttpMethod.POST,params,String.class);
        }catch (Exception e){
            System.out.println("请求失败"+e);
        }
        String base64String = JSON.parseObject(response.getBody()).getString("images");
        base64String = base64String.replace("[\"","");
        base64String = base64String.replace("\"]","");
        return getImageUrl(base64String);

    }
    private String getImageUrl(String base64String){
        String newName = System.currentTimeMillis()+"";
        String imagePath = "***" + newName + ".png"; // 指定保存图片的路径和文件名

        try {
            // 解码Base64字符串为字节数组
            byte[] imageBytes = Base64.getDecoder().decode(base64String);

            // 创建输出流将字节数组写入文件
            FileOutputStream fos = new FileOutputStream(imagePath);
            fos.write(imageBytes);
            fos.close();

            System.out.println("Image saved successfully.");
        } catch (IOException e) {
            System.out.println("Error occurred while saving image: " + e.getMessage());
        }
        return "http://localhost:8085/img/"+newName+".png";
    }
    public String encodeFileToBase64(String filePath) throws IOException {
        File file = new File(filePath);
        byte[] fileBytes = new byte[(int) file.length()];

        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(fileBytes);
        }

        return Base64.getEncoder().encodeToString(fileBytes);
    }
