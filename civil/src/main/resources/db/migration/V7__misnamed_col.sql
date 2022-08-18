ALTER TABLE topics
ADD COLUMN thumb_img_url text;

ALTER TABLE topics
DROP COLUMN thumbImgUrl;